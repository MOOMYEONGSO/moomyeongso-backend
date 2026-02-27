package org.example.moomyeongso.domain.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.moomyeongso.domain.post.entity.Post;
import org.example.moomyeongso.domain.readhistory.entity.ReadHistory;
import org.example.moomyeongso.domain.readhistory.repository.ReadHistoryRepository;
import org.example.moomyeongso.domain.user.entity.Streak;
import org.example.moomyeongso.domain.user.entity.User;
import org.example.moomyeongso.domain.user.repository.UserRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.example.moomyeongso.common.util.TimeUtils.KST;

@Service
@Slf4j
@RequiredArgsConstructor
public class MigrationService {

    private final MongoTemplate mongoTemplate;
    private final UserRepository userRepository;
    private final ReadHistoryRepository readHistoryRepository;

    public void migrateAnonymousData(String fromUserId, String toUserId) {
        if (fromUserId == null || toUserId == null || fromUserId.equals(toUserId)) {
            return;
        }

        migratePosts(fromUserId, toUserId);
        migrateCoin(fromUserId, toUserId);
        migrateReadHistory(fromUserId, toUserId);
        migrateStreak(fromUserId, toUserId);
    }

    public long migratePosts(String fromUserId, String toUserId) {
        if (fromUserId == null || toUserId == null || fromUserId.equals(toUserId)) {
            return 0;
        }

        Query query = Query.query(Criteria.where("userId").is(fromUserId));
        Update update = new Update().set("userId", toUserId);

        long modified = mongoTemplate.updateMulti(query, update, Post.class).getModifiedCount();
        log.info("Post ownership migrated: fromUserId={}, toUserId={}, modified={}",
                fromUserId, toUserId, modified);
        return modified;
    }

    private void migrateCoin(String fromUserId, String toUserId) {
        User anonymousUser = userRepository.findById(fromUserId).orElse(null);
        if (anonymousUser == null) {
            return;
        }

        int coinToTransfer = Math.max(anonymousUser.getCoin(), 0);
        if (coinToTransfer <= 0) {
            return;
        }

        Query memberQuery = Query.query(Criteria.where("_id").is(toUserId));
        Update addCoin = new Update().inc("coin", coinToTransfer);
        mongoTemplate.updateFirst(memberQuery, addCoin, User.class);

        log.info("Coin migrated: fromUserId={}, toUserId={}, amount={}",
                fromUserId, toUserId, coinToTransfer);
    }

    private void migrateReadHistory(String fromUserId, String toUserId) {
        List<ReadHistory> anonymousHistories = readHistoryRepository.findAllByUserIdOrderByReadAtDesc(fromUserId);

        int moved = 0;
        int merged = 0;

        for (ReadHistory anonymousHistory : anonymousHistories) {
            Optional<ReadHistory> existingOpt =
                    readHistoryRepository.findByUserIdAndPostId(toUserId, anonymousHistory.getPostId());

            if (existingOpt.isPresent()) {
                ReadHistory existing = existingOpt.get();
                LocalDateTime latest = latest(existing.getReadAt(), anonymousHistory.getReadAt());
                if (!Objects.equals(latest, existing.getReadAt())) {
                    mongoTemplate.updateFirst(
                            Query.query(Criteria.where("_id").is(existing.getId())),
                            new Update().set("readAt", latest),
                            ReadHistory.class
                    );
                }
                readHistoryRepository.deleteById(anonymousHistory.getId());
                merged++;
                continue;
            }

            mongoTemplate.updateFirst(
                    Query.query(Criteria.where("_id").is(anonymousHistory.getId())),
                    new Update().set("userId", toUserId),
                    ReadHistory.class
            );
            moved++;
        }

        log.info("Read history migrated: fromUserId={}, toUserId={}, moved={}, merged={}",
                fromUserId, toUserId, moved, merged);
    }

    private void migrateStreak(String fromUserId, String toUserId) {
        User anonymousUser = userRepository.findById(fromUserId).orElse(null);
        User memberUser = userRepository.findById(toUserId).orElse(null);
        if (anonymousUser == null || memberUser == null) {
            return;
        }

        Streak anonymous = anonymousUser.getStreak();
        Streak member = memberUser.getStreak();

        int anonymousCurrent = anonymous == null ? 0 : anonymous.getCurrent();
        int memberCurrent = member == null ? 0 : member.getCurrent();
        int anonymousBest = anonymous == null ? 0 : anonymous.getBest();
        int memberBest = member == null ? 0 : member.getBest();

        String anonymousDateStr = anonymous == null ? null : anonymous.getLastSeenDate();
        String memberDateStr = member == null ? null : member.getLastSeenDate();
        LocalDate anonymousDate = parseDateOrNull(anonymousDateStr);
        LocalDate memberDate = parseDateOrNull(memberDateStr);

        LocalDate mergedDate = pickLater(memberDate, anonymousDate);
        String mergedDateStr = mergedDate == null ? null : mergedDate.toString();

        int mergedCurrent;
        if (memberDate != null && anonymousDate != null && memberDate.equals(anonymousDate)) {
            mergedCurrent = Math.max(memberCurrent, anonymousCurrent);
        } else if (mergedDate != null && mergedDate.equals(anonymousDate)) {
            mergedCurrent = anonymousCurrent;
        } else {
            mergedCurrent = memberCurrent;
        }

        int mergedBest = Math.max(memberBest, anonymousBest);
        boolean mergedTodayMarked = mergedDate != null && mergedDate.equals(LocalDate.now(KST));

        Update streakUpdate = new Update()
                .set("streak.current", mergedCurrent)
                .set("streak.best", mergedBest)
                .set("streak.lastSeenDate", mergedDateStr)
                .set("streak.todayMarked", mergedTodayMarked);

        mongoTemplate.updateFirst(
                Query.query(Criteria.where("_id").is(toUserId)),
                streakUpdate,
                User.class
        );

        log.info("Streak migrated: fromUserId={}, toUserId={}, current={}, best={}, lastSeenDate={}",
                fromUserId, toUserId, mergedCurrent, mergedBest, mergedDateStr);
    }

    private LocalDate pickLater(LocalDate first, LocalDate second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return first.isAfter(second) ? first : second;
    }

    private LocalDate parseDateOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            log.warn("Invalid streak date format: {}", value);
            return null;
        }
    }

    private LocalDateTime latest(LocalDateTime a, LocalDateTime b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.isAfter(b) ? a : b;
    }
}
