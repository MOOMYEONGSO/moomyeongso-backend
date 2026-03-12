package org.example.moomyeongso.domain.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.moomyeongso.common.exception.CustomException;
import org.example.moomyeongso.common.exception.ErrorCode;
import org.example.moomyeongso.domain.post.entity.Post;
import org.example.moomyeongso.domain.readhistory.entity.ReadHistory;
import org.example.moomyeongso.domain.readhistory.repository.ReadHistoryRepository;
import org.example.moomyeongso.domain.user.entity.User;
import org.example.moomyeongso.domain.user.entity.UserRole;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class MigrationService {

    private final MongoTemplate mongoTemplate;
    private final ReadHistoryRepository readHistoryRepository;

    public Optional<User> consumeAnonymousUserForMigration(String anonymousUserId) {
        if (anonymousUserId == null) {
            return Optional.empty();
        }

        Query claimQuery = Query.query(Criteria.where("_id").is(anonymousUserId)
                .and("userRole").is(UserRole.ANONYMOUS));
        User consumed = mongoTemplate.findAndRemove(claimQuery, User.class);

        if (consumed != null) {
            log.info("Anonymous user consumed for migration: userId={}", anonymousUserId);
        }

        return Optional.ofNullable(consumed);
    }

    public void migrateAnonymousData(User anonymousUser, User memberUser) {
        if (anonymousUser == null || anonymousUser.getId() == null
                || memberUser == null || memberUser.getId() == null) {
            return;
        }

        String fromUserId = anonymousUser.getId();
        String toUserId = memberUser.getId();
        if (fromUserId.equals(toUserId)) {
            return;
        }

        migratePosts(fromUserId, toUserId);
        migrateCoin(anonymousUser, toUserId);
        migrateReadHistory(fromUserId, toUserId);
    }

    public void migratePosts(String fromUserId, String toUserId) {
        if (fromUserId == null || toUserId == null || fromUserId.equals(toUserId)) {
            return;
        }

        Query query = Query.query(Criteria.where("userId").is(fromUserId));
        Update update = new Update().set("userId", toUserId);

        long modified = mongoTemplate.updateMulti(query, update, Post.class).getModifiedCount();
        log.info("Post ownership migrated: fromUserId={}, toUserId={}, modified={}",
                fromUserId, toUserId, modified);
    }

    private void migrateCoin(User anonymousUser, String toUserId) {
        String fromUserId = anonymousUser.getId();
        int coinToTransfer = Math.max(anonymousUser.getCoin(), 0);
        if (coinToTransfer <= 0) {
            return;
        }

        Query memberQuery = Query.query(Criteria.where("_id").is(toUserId));
        Update addCoin = new Update().inc("coin", coinToTransfer);
        long matchedCount = mongoTemplate.updateFirst(memberQuery, addCoin, User.class).getMatchedCount();
        if (matchedCount == 0) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

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
