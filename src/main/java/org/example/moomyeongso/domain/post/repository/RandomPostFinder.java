package org.example.moomyeongso.domain.post.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import org.example.moomyeongso.domain.post.entity.Post;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.example.moomyeongso.domain.post.entity.PostStatus;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;

import java.util.List;
import java.util.Optional;
@Component
@RequiredArgsConstructor
public class RandomPostFinder {

    private final MongoTemplate mongoTemplate;

    public List<Post> findRandomByStatusExcludingUser(PostStatus status, int size, String excludedUserId) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(
                        Criteria.where("status").is(status)
                                .and("userId").ne(excludedUserId)
                ),
                Aggregation.sample(size)
        );

        AggregationResults<Post> result = mongoTemplate.aggregate(
                aggregation,
                Post.class,
                Post.class
        );

        return result.getMappedResults();
    }

    public List<Post> findRandomByStatusAndTagExcludingUser(PostStatus status, String tag, int size, String excludedUserId) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(
                        Criteria.where("status").is(status)
                                .and("tags").is(tag)
                                .and("userId").ne(excludedUserId)
                ),
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
