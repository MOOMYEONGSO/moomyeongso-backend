package org.example.moomyeongso.domain.post.repository;

import lombok.RequiredArgsConstructor;
import org.example.moomyeongso.domain.post.entity.Post;
import org.example.moomyeongso.domain.post.entity.PostStatus;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RandomPostFinder {

    private final MongoTemplate mongoTemplate;

    public List<Post> findRandomByStatusExcludingUser(PostStatus status, int size, String excludedUserId) {
        return findRandomByStatusExcludingUser(status, size, excludedUserId, List.of());
    }

    public List<Post> findRandomByStatusExcludingUser(
            PostStatus status,
            int size,
            String excludedUserId,
            List<String> excludedPostIds
    ) {
        Criteria criteria = Criteria.where("status").is(status)
                .and("userId").ne(excludedUserId);
        if (excludedPostIds != null && !excludedPostIds.isEmpty()) {
            criteria.and("_id").nin(excludedPostIds);
        }

        return aggregateRandom(criteria, size);
    }

    public List<Post> findRandomByStatusAndAnyTagExcludingUser(
            PostStatus status,
            List<String> tags,
            int size,
            String excludedUserId
    ) {
        Criteria criteria = Criteria.where("status").is(status)
                .and("tags").in(tags)
                .and("userId").ne(excludedUserId);

        return aggregateRandom(criteria, size);
    }

    public List<Post> findRandomByStatusAndTagExcludingUser(PostStatus status, String tag, int size, String excludedUserId) {
        return findRandomByStatusAndAnyTagExcludingUser(status, List.of(tag), size, excludedUserId);
    }

    private List<Post> aggregateRandom(Criteria criteria, int size) {
        if (size <= 0) {
            return List.of();
        }

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(criteria),
                Aggregation.sample(size)
        );

        AggregationResults<Post> result = mongoTemplate.aggregate(
                aggregation,
                Post.class,
                Post.class
        );

        return result.getMappedResults();
    }
}
