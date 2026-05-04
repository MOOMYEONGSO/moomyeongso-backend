package org.example.moomyeongso.domain.user.service;

import lombok.RequiredArgsConstructor;
import org.example.moomyeongso.common.exception.CustomException;
import org.example.moomyeongso.common.exception.ErrorCode;
import org.example.moomyeongso.domain.user.dto.request.VisitMotiveRequestDto;
import org.example.moomyeongso.domain.user.dto.response.UserInfoResponseDto;
import org.example.moomyeongso.domain.user.entity.User;
import org.example.moomyeongso.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public void updateNickname(String userId, String nickname) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!nickname.equals(user.getNickname()) && userRepository.existsByNickname(nickname)) {
            throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);
        }

        user.updateNickname(nickname);
        userRepository.save(user);
    }

    public void updateVisitMotive(String userId, VisitMotiveRequestDto request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        user.updateVisitMotive(request.visitMotive());
        userRepository.save(user);
    }

    public UserInfoResponseDto getMyInfo(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (user.getNickname() == null || user.getNickname().isBlank()) {
            throw new CustomException(ErrorCode.NICKNAME_NOT_FOUND);
        }

        return UserInfoResponseDto.from(user);
    }
}
