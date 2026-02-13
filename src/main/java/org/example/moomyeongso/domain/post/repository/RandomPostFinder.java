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

    public Optional<Post> find() {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.sample(1)
        );

        AggregationResults<Post> result = mongoTemplate.aggregate(
                aggregation,
                Post.class,
                Post.class
        );

        return result.getMappedResults().stream().findFirst();
    }

    public List<Post> findRandomByStatus(PostStatus status, int size) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("status").is(status)),
                Aggregation.sample(size)
        );

        AggregationResults<Post> result = mongoTemplate.aggregate(
                aggregation,
                Post.class,
                Post.class
        );

        return result.getMappedResults();
    }

    public List<Post> findRandomByStatusAndTag(PostStatus status, String tag, int size) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(
                        Criteria.where("status").is(status)
                                .and("tags").is(tag)
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
