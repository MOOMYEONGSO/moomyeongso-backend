package org.example.moomyeongso.domain.post.entity;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Getter
public enum PostTag {
    PEOPLE("PEOPLE", 0),
    FRIENDSHIP("FRIENDSHIP", 1),
    THOUGHTS("THOUGHTS", 2),
    HAPPY("HAPPY", 3),
    GRATITUDE("GRATITUDE", 4),
    TIME("TIME", 5),
    OTHER("OTHER", 6);

    private static final int DEFAULT_PRIORITY = Integer.MAX_VALUE;
    private static final Map<String, Integer> PRIORITY_BY_LABEL = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(PostTag::getLabel, PostTag::getPriority));

    private final String label;
    private final int priority;

    PostTag(String label, int priority) {
        this.label = label;
        this.priority = priority;
    }

    public static List<String> sortByPriority(List<String> tags) {
        if (tags == null || tags.isEmpty()) return List.of();

        return tags.stream()
                .filter(Objects::nonNull)
                .map(PostTag::normalizeLabel)
                .distinct()
                .sorted(Comparator.comparingInt(PostTag::resolvePriority))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private static String normalizeLabel(String label) {
        return label.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private static int resolvePriority(String label) {
        return PRIORITY_BY_LABEL.getOrDefault(label, DEFAULT_PRIORITY);
    }
}
