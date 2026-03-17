package org.example.moomyeongso.domain.post.service;

import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.example.moomyeongso.domain.post.entity.PostComment;
import org.example.moomyeongso.domain.post.entity.PostCommentStatus;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostCommentService {

    private final MongoTemplate mongoTemplate;

    public Map<String, Long> getActiveCommentCounts(List<String> postIds) {
        if (postIds == null || postIds.isEmpty()) {
            return Map.of();
        }

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(
                        Criteria.where("postId").in(postIds)
                                .and("status").is(PostCommentStatus.ACTIVE)
                ),
                Aggregation.group("postId").count().as("count"),
                Aggregation.project("count").and("_id").as("postId")
        );

        AggregationResults<Document> results =
                mongoTemplate.aggregate(aggregation, PostComment.class, Document.class);

        return results.getMappedResults().stream()
                .collect(Collectors.toMap(
                        doc -> doc.getString("postId"),
                        doc -> ((Number) doc.get("count")).longValue()
                ));
    }
}
