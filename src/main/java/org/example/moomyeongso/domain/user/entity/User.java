package org.example.moomyeongso.domain.user.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDate;
import java.time.LocalDateTime;
import static org.example.moomyeongso.common.util.TimeUtils.KST;

@Document(collection = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User {

    @Id
    private String id;

    @Indexed(unique = true, sparse = true)
    private String nickname;

    private String passwordHash;

    @Builder.Default
    private int coin = 0;

    // USER / ANONYMOUS 회원, 비회원 구분
    private UserRole userRole;

    // ACTIVE / WITHDRAWN / BANNED 같은 상태
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Field("streak")
    @Builder.Default
    private Streak streak = Streak.builder()
            .current(1)
            .best(1)
            .lastSeenDate(LocalDate.now(KST).toString())
            .todayMarked(true)
            .build();

    private VisitMotive visitMotive;

    @CreatedDate
    private LocalDateTime createdAt;

    // 익명 로그인시 TTL 만료 정책을 위한 컬럼
    private LocalDateTime expiresAt;

    /* CoinService의 원자 연산으로 변경
    public void addCoin(int amount) {
        this.coin += amount;
    }

    public void decreaseCoin(int amount){
        if (this.coin < amount) {
            throw new CustomException(ErrorCode.NOT_ENOUGH_COIN);
        }
        this.coin -= amount;
    }
    */
    // 익명 -> 회원가입 시 업데이트
    public void updateToMember(String nickname, String passwordHash) {
        this.nickname = nickname;
        this.passwordHash = passwordHash;
        this.userRole = UserRole.USER;
        this.expiresAt = null; // 회원은 TTL 만료 대상에서 제외
    }

    //닉네임 적용
    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void updateVisitMotive(VisitMotive visitMotive) {
        this.visitMotive = visitMotive;
    }

    public void changeStatus(UserStatus status) {
        this.status = status;
    }

}
