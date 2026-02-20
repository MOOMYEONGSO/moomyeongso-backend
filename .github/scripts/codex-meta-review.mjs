import fs from "node:fs/promises";

const DEFAULT_MARKER = "<!-- codex-meta-review -->";
const REQUIRED_HEADERS = [
  "Title: codex meta review",
  "Section: merge blocker (must-fix)",
  "Section: should-fix",
  "Section: optional",
  "Section: gemini validation summary",
  "Section: additional risks or test suggestions",
];

const {
  GITHUB_TOKEN,
  OPENAI_API_KEY,
  OPENAI_MODEL,
  GEMINI_BOT_LOGINS,
  REPOSITORY,
  PR_NUMBER,
  API_URL = "https://api.github.com",
  SERVER_URL = "https://github.com",
  MAX_GEMINI_ITEMS = "300",
  MAX_CHANGED_FILES = "80",
  MAX_PATCH_CHARS = "1400",
  MAX_COMMENT_BODY_CHARS = "4000",
  WAIT_FOR_GEMINI_SECONDS = "120",
} = process.env;

if (!GITHUB_TOKEN) {
  throw new Error("GITHUB_TOKEN is required.");
}
if (!REPOSITORY || !REPOSITORY.includes("/")) {
  throw new Error("REPOSITORY must be provided in owner/repo format.");
}
if (!PR_NUMBER || Number.isNaN(Number(PR_NUMBER))) {
  throw new Error("PR_NUMBER must be a valid pull request number.");
}

const [owner, repo] = REPOSITORY.split("/");
const prNumber = Number(PR_NUMBER);

const maxGeminiItems = parsePositiveInt(MAX_GEMINI_ITEMS, 300);
const maxChangedFiles = parsePositiveInt(MAX_CHANGED_FILES, 80);
const maxPatchChars = parsePositiveInt(MAX_PATCH_CHARS, 1400);
const maxCommentBodyChars = parsePositiveInt(MAX_COMMENT_BODY_CHARS, 4000);
const waitForGeminiSeconds = parseNonNegativeInt(WAIT_FOR_GEMINI_SECONDS, 120);

const geminiLogins = parseLogins(
  GEMINI_BOT_LOGINS,
  ["gemini-code-assist[bot]", "google-gemini-code-assist[bot]", "gemini[bot]"],
);

const configText = await readIfExists(".codex/config.yaml");
const marker = readMarkerFromConfig(configText) ?? DEFAULT_MARKER;

const promptText =
  (await readIfExists(".codex/meta-review-prompt.md")) ??
  `너는 PR 메타 리뷰어다. Gemini 리뷰를 검증하고 우선순위를 재정렬하여 단일 코멘트를 만든다.`;

const pr = await ghRequest(`/repos/${owner}/${repo}/pulls/${prNumber}`);
let [reviews, reviewComments, issueComments, changedFiles] = await Promise.all([
  ghPaginate(`/repos/${owner}/${repo}/pulls/${prNumber}/reviews?per_page=100`),
  ghPaginate(`/repos/${owner}/${repo}/pulls/${prNumber}/comments?per_page=100`),
  ghPaginate(`/repos/${owner}/${repo}/issues/${prNumber}/comments?per_page=100`),
  ghPaginate(`/repos/${owner}/${repo}/pulls/${prNumber}/files?per_page=100`),
]);

let geminiFeedback = collectGeminiFeedback({
  reviews,
  reviewComments,
  issueComments,
  maxCommentBodyChars,
  maxGeminiItems,
  geminiLogins,
});

if (geminiFeedback.length === 0 && waitForGeminiSeconds > 0) {
  const deadline = Date.now() + waitForGeminiSeconds * 1000;
  while (Date.now() < deadline) {
    await sleep(20_000);
    [reviews, reviewComments, issueComments] = await Promise.all([
      ghPaginate(`/repos/${owner}/${repo}/pulls/${prNumber}/reviews?per_page=100`),
      ghPaginate(`/repos/${owner}/${repo}/pulls/${prNumber}/comments?per_page=100`),
      ghPaginate(`/repos/${owner}/${repo}/issues/${prNumber}/comments?per_page=100`),
    ]);

    geminiFeedback = collectGeminiFeedback({
      reviews,
      reviewComments,
      issueComments,
      maxCommentBodyChars,
      maxGeminiItems,
      geminiLogins,
    });

    if (geminiFeedback.length > 0) {
      break;
    }
  }
}

const normalizedChangedFiles = changedFiles
  .slice()
  .sort((a, b) => String(a.filename).localeCompare(String(b.filename)))
  .slice(0, maxChangedFiles)
  .map((file) => ({
    filename: file.filename,
    status: file.status,
    additions: file.additions,
    deletions: file.deletions,
    changes: file.changes,
    patch: clip(file.patch ?? "", maxPatchChars),
  }));

const requestPayload = {
  pr: {
    number: pr.number,
    title: pr.title,
    body: clip(pr.body ?? "", maxCommentBodyChars),
    base: pr.base?.ref ?? "",
    head: pr.head?.ref ?? "",
    changed_files: pr.changed_files,
    additions: pr.additions,
    deletions: pr.deletions,
    html_url: pr.html_url ?? `${SERVER_URL}/${owner}/${repo}/pull/${prNumber}`,
  },
  gemini: {
    available: geminiFeedback.length > 0,
    total_items: geminiFeedback.length,
    items: geminiFeedback,
  },
  changed_files: normalizedChangedFiles,
};

let metaCommentBody;

if (OPENAI_API_KEY && OPENAI_API_KEY.trim().length > 0) {
  try {
    const firstDraft = await runOpenAI(promptText, buildUserPrompt(requestPayload), OPENAI_MODEL);
    const repaired = hasRequiredHeaders(firstDraft)
      ? firstDraft
      : await runOpenAI(
          "아래 출력은 포맷이 맞지 않는다. 지정된 섹션 구조를 유지해 한국어로 다시 작성하라.",
          buildRepairPrompt(firstDraft),
          OPENAI_MODEL,
        );

    if (hasRequiredHeaders(repaired)) {
      metaCommentBody = ensureManagedComment(repaired, marker);
    } else {
      metaCommentBody = buildFallbackComment({
        marker,
        reason: "모델 출력 포맷 검증 실패",
        requestPayload,
      });
    }
  } catch (error) {
    metaCommentBody = buildFallbackComment({
      marker,
      reason: `OpenAI 호출 실패: ${clip(error?.message ?? "unknown error", 220)}`,
      requestPayload,
    });
  }
} else {
  metaCommentBody = buildFallbackComment({
    marker,
    reason: "OPENAI_API_KEY 미설정",
    requestPayload,
  });
}

await upsertMetaReviewComment({
  owner,
  repo,
  prNumber,
  marker,
  issueComments,
  body: metaCommentBody,
});

console.log(
  JSON.stringify({
    pr: prNumber,
    geminiFeedbackCount: geminiFeedback.length,
    changedFilesUsed: normalizedChangedFiles.length,
    usedOpenAI: Boolean(OPENAI_API_KEY && OPENAI_API_KEY.trim().length > 0),
    commentLength: metaCommentBody.length,
  }),
);

function parsePositiveInt(value, fallback) {
  if (value == null || String(value).trim().length === 0) {
    return fallback;
  }
  const parsed = Number(value);
  if (!Number.isFinite(parsed) || parsed <= 0) {
    return fallback;
  }
  return Math.floor(parsed);
}

function parseNonNegativeInt(value, fallback) {
  if (value == null || String(value).trim().length === 0) {
    return fallback;
  }
  const parsed = Number(value);
  if (!Number.isFinite(parsed) || parsed < 0) {
    return fallback;
  }
  return Math.floor(parsed);
}

function parseLogins(value, defaults) {
  const source = value && value.trim().length > 0 ? value : defaults.join(",");
  return new Set(
    source
      .split(",")
      .map((item) => item.trim().toLowerCase())
      .filter(Boolean),
  );
}

function clip(value, maxChars) {
  if (!value) {
    return "";
  }
  const text = String(value).replace(/\r/g, "").trim();
  if (text.length <= maxChars) {
    return text;
  }
  return `${text.slice(0, maxChars)} ...[truncated]`;
}

async function readIfExists(path) {
  try {
    return await fs.readFile(path, "utf8");
  } catch (error) {
    if (error?.code === "ENOENT") {
      return null;
    }
    throw error;
  }
}

function readMarkerFromConfig(config) {
  if (!config) {
    return null;
  }
  const matched = config.match(/^\s*marker:\s*["']?(.*?)["']?\s*$/m);
  return matched?.[1]?.trim() || null;
}

function compareTimeThenId(timeA, timeB, idA, idB) {
  const parsedA = Date.parse(timeA ?? "");
  const parsedB = Date.parse(timeB ?? "");
  const a = Number.isFinite(parsedA) ? parsedA : 0;
  const b = Number.isFinite(parsedB) ? parsedB : 0;
  if (a !== b) {
    return a - b;
  }
  return Number(idA ?? 0) - Number(idB ?? 0);
}

function comparePathLineId(a, b) {
  const pathA = String(a.path ?? "");
  const pathB = String(b.path ?? "");
  if (pathA !== pathB) {
    return pathA.localeCompare(pathB);
  }

  const lineA = Number(a.line ?? a.original_line ?? 0);
  const lineB = Number(b.line ?? b.original_line ?? 0);
  if (lineA !== lineB) {
    return lineA - lineB;
  }

  return Number(a.id ?? 0) - Number(b.id ?? 0);
}

function normalizeWhitespace(value) {
  return String(value ?? "")
    .replace(/\s+/g, " ")
    .trim()
    .toLowerCase();
}

function dedupeFeedback(items) {
  const seen = new Set();
  const result = [];
  for (const item of items) {
    const key = [
      item.type,
      normalizeWhitespace(item.body),
      item.path ?? "",
      item.line ?? "",
      item.side ?? "",
    ].join("|");
    if (seen.has(key)) {
      continue;
    }
    seen.add(key);
    result.push(item);
  }
  return result;
}

function collectGeminiFeedback({ reviews, reviewComments, issueComments, maxCommentBodyChars, maxGeminiItems, geminiLogins }) {
  const sortedReviewBodies = reviews
    .slice()
    .sort((a, b) => compareTimeThenId(a.submitted_at ?? a.created_at, b.submitted_at ?? b.created_at, a.id, b.id))
    .filter((review) => isGeminiAuthor(review.user?.login, geminiLogins))
    .map((review) => ({
      type: "review_summary",
      id: review.id,
      created_at: review.submitted_at ?? review.created_at ?? "",
      body: clip(review.body, maxCommentBodyChars),
    }))
    .filter((item) => item.body.length > 0);

  const sortedIssueCommentBodies = issueComments
    .slice()
    .sort((a, b) => compareTimeThenId(a.created_at, b.created_at, a.id, b.id))
    .filter((comment) => isGeminiAuthor(comment.user?.login, geminiLogins))
    .map((comment) => ({
      type: "issue_comment",
      id: comment.id,
      created_at: comment.created_at ?? "",
      body: clip(comment.body, maxCommentBodyChars),
    }))
    .filter((item) => item.body.length > 0);

  const sortedInlineComments = reviewComments
    .slice()
    .sort((a, b) => comparePathLineId(a, b))
    .filter((comment) => isGeminiAuthor(comment.user?.login, geminiLogins))
    .map((comment) => ({
      type: "inline_comment",
      id: comment.id,
      path: comment.path ?? "",
      line: comment.line ?? comment.original_line ?? null,
      side: comment.side ?? "",
      commit_id: comment.commit_id ?? "",
      body: clip(comment.body, maxCommentBodyChars),
    }))
    .filter((item) => item.body.length > 0);

  return dedupeFeedback([
    ...sortedReviewBodies,
    ...sortedIssueCommentBodies,
    ...sortedInlineComments,
  ]).slice(0, maxGeminiItems);
}

function isGeminiAuthor(login, allowedLogins) {
  const normalized = String(login ?? "").toLowerCase();
  return normalized.length > 0 && allowedLogins.has(normalized);
}

function buildUserPrompt(payload) {
  return [
    "다음 데이터로 PR 메타 리뷰 코멘트를 작성하라.",
    "출력은 한국어로 작성한다.",
    "형식은 반드시 Title/Section 구조를 유지한다.",
    "Gemini 피드백이 없으면 그 사실을 명시하고 PR diff 기반으로 작성한다.",
    "",
    "INPUT_JSON:",
    JSON.stringify(payload, null, 2),
  ].join("\n");
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function buildRepairPrompt(rawText) {
  return [
    "다음 출력 결과를 엄격한 형식으로 재작성하라.",
    "필수 헤더:",
    ...REQUIRED_HEADERS,
    "",
    "원본:",
    rawText,
  ].join("\n");
}

function hasRequiredHeaders(text) {
  const normalized = String(text ?? "");
  return REQUIRED_HEADERS.every((header) => normalized.includes(header));
}

function ensureManagedComment(text, markerText) {
  const body = String(text ?? "").trim();
  if (!body.includes("Title: codex meta review")) {
    return buildFallbackComment({
      marker: markerText,
      reason: "타이틀 누락",
      requestPayload: null,
    });
  }
  if (body.startsWith(markerText)) {
    return body;
  }
  return `${markerText}\n${body}`;
}

function buildFallbackComment({ marker: markerText, reason, requestPayload }) {
  const geminiAvailable = Boolean(requestPayload?.gemini?.available);
  const items = requestPayload?.gemini?.items ?? [];
  const topItems = items.slice(0, 5);
  const shouldFixLines =
    topItems.length > 0
      ? topItems.map((item) => {
          const location = item.path ? `\`${item.path}${item.line ? `:${item.line}` : ""}\` ` : "";
          return `• [needs verification] ${location}${clip(item.body, 220)}`;
        })
      : ["• 없음"];

  const agreement = geminiAvailable
    ? "Gemini 피드백을 수집했으나 자동 검증 경로에서 제한이 발생했다."
    : "Gemini 피드백이 없거나 수집되지 않았다.";

  return [
    markerText,
    "Title: codex meta review",
    "",
    "Section: merge blocker (must-fix)",
    "• 없음",
    "",
    "Section: should-fix",
    ...shouldFixLines,
    "",
    "Section: optional",
    "• 자동 메타 분석이 제한되어 상세 분류는 needs verification",
    "",
    "Section: gemini validation summary",
    `• Agreement: ${agreement}`,
    `• Rebuttal or refinement: ${reason} (needs verification)`,
    "",
    "Section: additional risks or test suggestions",
    "• 성능, 보안, 경계값, 운영 알림/로그 검증 테스트를 권장 (needs verification)",
  ].join("\n");
}

async function runOpenAI(systemPrompt, userPrompt, modelName) {
  const model = modelName && modelName.trim().length > 0 ? modelName.trim() : "gpt-5-mini";
  const response = await fetch("https://api.openai.com/v1/responses", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${OPENAI_API_KEY}`,
    },
    body: JSON.stringify({
      model,
      input: [
        {
          role: "system",
          content: [{ type: "input_text", text: systemPrompt }],
        },
        {
          role: "user",
          content: [{ type: "input_text", text: userPrompt }],
        },
      ],
    }),
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(`OpenAI API failed (${response.status}): ${text}`);
  }

  const data = await response.json();
  const outputText = extractOutputText(data);
  if (!outputText) {
    throw new Error("OpenAI response did not include text output.");
  }
  return outputText.trim();
}

function extractOutputText(data) {
  if (typeof data?.output_text === "string" && data.output_text.trim().length > 0) {
    return data.output_text;
  }

  if (!Array.isArray(data?.output)) {
    return "";
  }

  const parts = [];
  for (const item of data.output) {
    if (!Array.isArray(item?.content)) {
      continue;
    }
    for (const content of item.content) {
      if (typeof content?.text === "string" && content.text.trim().length > 0) {
        parts.push(content.text);
      }
    }
  }
  return parts.join("\n").trim();
}

async function upsertMetaReviewComment({ owner: repoOwner, repo: repoName, prNumber: number, marker: markerText, issueComments: comments, body }) {
  const existing = comments
    .slice()
    .reverse()
    .find((comment) => String(comment.body ?? "").includes(markerText));

  if (existing) {
    await ghRequest(`/repos/${repoOwner}/${repoName}/issues/comments/${existing.id}`, {
      method: "PATCH",
      body: { body },
    });
    return;
  }

  await ghRequest(`/repos/${repoOwner}/${repoName}/issues/${number}/comments`, {
    method: "POST",
    body: { body },
  });
}

async function ghPaginate(pathWithQuery) {
  const all = [];
  let page = 1;
  while (true) {
    const separator = pathWithQuery.includes("?") ? "&" : "?";
    const pagedPath = `${pathWithQuery}${separator}page=${page}`;
    const pageData = await ghRequest(pagedPath);
    if (!Array.isArray(pageData)) {
      throw new Error(`Expected paginated array for ${pagedPath}`);
    }
    all.push(...pageData);
    if (pageData.length < 100) {
      break;
    }
    page += 1;
  }
  return all;
}

async function ghRequest(pathOrUrl, options = {}) {
  const url = pathOrUrl.startsWith("http") ? pathOrUrl : `${API_URL}${pathOrUrl}`;
  const method = options.method ?? "GET";
  const response = await fetch(url, {
    method,
    headers: {
      Accept: "application/vnd.github+json",
      Authorization: `Bearer ${GITHUB_TOKEN}`,
      "X-GitHub-Api-Version": "2022-11-28",
      ...(options.headers ?? {}),
    },
    body: options.body ? JSON.stringify(options.body) : undefined,
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(`GitHub API failed (${response.status}) ${method} ${url}: ${text}`);
  }

  const contentType = response.headers.get("content-type") ?? "";
  if (!contentType.includes("application/json")) {
    return null;
  }
  return response.json();
}
