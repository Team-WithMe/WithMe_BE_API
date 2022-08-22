package com.withme.api.controller.dto;

import com.withme.api.domain.team.Team;
import com.withme.api.domain.user.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "마이페이지 유저 및 팀 정보 응답 DTO 객체")
@Getter
@ToString
@NoArgsConstructor
public class MyPageResponseDto {

    @Schema(description = "유저 닉네임", example = "vV위드미Vv")
    private String nickname;

    @Schema(description = "유저 사진 경로", example = "null 혹은 경로")
    private String userImage;

    @Schema(description = "유저가 속한 팀 리스트", example = "???")
//    private List<Team> teamList = new ArrayList<>();
    private List<TeamResponseDto> teamList = new ArrayList<>();

    public MyPageResponseDto(User user, List<Team> teamList) {
        this.nickname = user.getNickname();
        this.userImage = user.getUserImage();
        teamList.forEach(team -> {
            this.teamList.add(new TeamResponseDto(team));
        });
    }

}
