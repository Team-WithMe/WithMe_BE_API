package com.withme.api.service;

import com.withme.api.controller.dto.*;
import com.withme.api.domain.skill.Skill;
import com.withme.api.domain.team.*;
import com.withme.api.domain.teamComment.TeamComment;
import com.withme.api.domain.teamComment.TeamCommentRepository;
import com.withme.api.domain.teamLike.TeamLike;
import com.withme.api.domain.teamLike.TeamLikeRepository;
import com.withme.api.domain.teamNotice.TeamNotice;
import com.withme.api.domain.teamNotice.TeamNoticeRepository;
import com.withme.api.domain.teamSkill.TeamSkill;
import com.withme.api.domain.teamSkill.TeamSkillRepository;
import com.withme.api.domain.teamUser.MemberType;
import com.withme.api.domain.teamUser.TeamUser;
import com.withme.api.domain.teamUser.TeamUserRepository;
import com.withme.api.domain.user.User;
import com.withme.api.domain.user.UserRepository;
import com.withme.api.jwt.TokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;

    private final TeamUserRepository teamUserRepository;

    private final TokenProvider tokenProvider;
    private final TeamNoticeRepository teamNoticeRepository;

    private final TeamCommentRepository teamCommentRepository;
    private final TeamSkillRepository teamSkillRepository;

    private final TeamLikeRepository teamLikeRepository;

    @Transactional
    public List<TeamListResponseDto> getTeamList(TeamSearchDto teamSearchDto) throws Exception {

        List<Team> teamList = new ArrayList<>();

        // NOTE 검색 조건 사용을 위해 TeamSkill 조회
        List<TeamSkill> teamSkills = teamSkillRepository.findAll();

        // NOTE 스킬 입력
        List<Skill> skillList = teamSearchDto.toSkillList();
        // NOTE 검색 조건 만들기
        List<TeamSkill> teamSkillList = toTeamListParams(skillList, teamSkills);

        // NOTE 검색 조건에 맞는 teamSkill이 없고 검색 조건의 Skill이 없으면
        if (teamSkillList.size() == 0 && skillList.size() == 0){
            // NOTE 내림 차순
            if (teamSearchDto.getSort() == 0){
                teamList = teamRepository.findAllByStatusOrderByCreatedTimeDesc(Status.DISPLAYED).orElseThrow(
                        () -> new NullPointerException("팀 조회 오류 (검색X)")
                );
            }else {
                teamList = teamRepository.findAllByStatusOrderByCreatedTimeAsc(Status.DISPLAYED).orElseThrow(
                        () -> new NullPointerException("팀 조회 오류 (검색X)")
                );
            }
        }else{
            if (teamSearchDto.getSort() == 0){
                teamList = teamRepository.findDistinctTeamsByTeamSkillsInAndStatusOrderByCreatedTimeDesc(teamSkillList, Status.DISPLAYED)
                        .orElseThrow(
                                () -> new NullPointerException("팀 조회 오류 (검색O)")
                        );
            }else {
                teamList = teamRepository.findDistinctTeamsByTeamSkillsInAndStatusOrderByCreatedTimeAsc(teamSkillList, Status.DISPLAYED)
                        .orElseThrow(
                                () -> new NullPointerException("팀 조회 오류 (검색O)")
                        );
            }
        }

        return teamList.stream()
                .map(TeamListResponseDto::new)
                .collect(Collectors.toList());
    }
    // NOTE 팀 검색 조건 처리 로직
    public List<TeamSkill> toTeamListParams(List<Skill> skills, List<TeamSkill> teamSkills) {
        List<TeamSkill> teamSkillsParams = new ArrayList<>();
        List<List<TeamSkill>> teamSkillsParamsList = new ArrayList<>();
        // NOTE 검색 조건 걸러냄
        for (Skill skill : skills) {
            teamSkillsParams = teamSkills.stream()
                    .filter(teamSkill -> {
                        return teamSkill.getSkill().getSkillName().equals(skill.getSkillName());
                    })
                    .collect(Collectors.toList());
            teamSkillsParamsList.add(teamSkillsParams);
        }

        // NOTE 검색 조건을 위해 리스트를 합침
        List<TeamSkill> params = teamSkillsParamsList.stream()
                .flatMap(x -> x.stream())
                .collect(Collectors.toList());

        return params;
    }

    /**
     * 팀 등록
     * */
    @Transactional
    public Long createTeam(CreateTeamRequestDto createTeamDto
            , String authHeader
    ) {
        // NOTE 현재 접속한 유저 ID 구해서 적용필요
        Long user_idx = tokenProvider.getUserIdFromToken(authHeader);
        // NOTE 팀으로 변경
        //Long user_idx = 1L;
        Team team = createTeamDto.setTeamSkill();

        User user = userRepository.findById(user_idx).orElseThrow(
                ()-> new NullPointerException("존재하지않는 사용자"));

        int teamNameUniqueCheck = teamRepository.countTeamByTeamNameEquals(team.getTeamName());

        // NOTE 팀 중복여부 확인
        if (teamNameUniqueCheck > 0) throw new DuplicateKeyException("팀등록시 팀이름이 중복");

        log.info("team ::: " + user);
        // NOTE 팀등록
        TeamUser teamUser = TeamUser.builder()
                .memberType(MemberType.LEADER)
                .team(team)
                .user(user)
                .build();
        team.addTeamUser(teamUser);
        Team returnTeam = teamRepository.save(team);

        return returnTeam.getId();
    }

    @Transactional
    public TeamNotice createTeamNotice(Long teamId, TeamNoticeCreateRequestDto dto, String authHeader) {
        /**
         * 1. authHeader 파싱해서 user id 가져오기
         * 2. team에 uesr가 있는지 확인하기 -> 없으면 exception 발생
         * 3. team에 공지사항 등록
         */

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team Id not exist."));

        Long userIdFromToken = tokenProvider.getUserIdFromToken(authHeader);
        team.isUserJoined(userIdFromToken);

        User user = userRepository.findById(userIdFromToken)
                .orElseThrow(() -> new UsernameNotFoundException("User not exist."));

        return teamNoticeRepository.save(dto.toEntity(team, user));
    }
    /**
     * 팀 게시물 상세 정보 조회
     * */
    @Transactional
    public TeamDetailResponseDto getTeamListByTeamId(Long teamId) {
        Team resultTeam = teamRepository.findTeamById(teamId)
                .orElseThrow(() -> new NullPointerException("Team not found"));

        TeamUser teamUser = teamUserRepository.findTeamUserByTeamAndMemberType(resultTeam, MemberType.LEADER)
                .orElseThrow(() -> new NullPointerException("TeamUser not found"));

        // NOTE 조회수 증가
        resultTeam.addViewCount();

        List<TeamComment> teamComments = teamCommentRepository.findTeamCommentByTeamAndParentIsNullOrderByIdDesc(resultTeam).get();

        List<TeamCommentResponseDto> teamCommentResponseDtos = teamComments.stream()
                .map(TeamCommentResponseDto::new)
                .collect(Collectors.toList());

        TeamDetailResponseDto resultTeamDto = new TeamDetailResponseDto(resultTeam, teamCommentResponseDtos, teamUser);
        getTeamAndComment(teamId, resultTeamDto);

        return resultTeamDto;
    }
    /**
     * 팀상세 게시글 댓글 조회 상세 로직
     * */
    public void getTeamAndComment(Long teamId, TeamDetailResponseDto dto) {
        dto.getTeamComments()
                .forEach(teamComment -> {
                    List<TeamComment> teamComments = teamCommentRepository.findTeamCommentsByTeamIdAndId(teamId, teamComment.getId());
                    List<TeamChildrenCommentResponse> teamChildrenCommentResponses = teamComments.stream()
                            .map(TeamChildrenCommentResponse::new)
                            .collect(Collectors.toList());
                    teamComment.setCommentChildren(teamChildrenCommentResponses);
                });
    }

    /**
     * 팀 게시물 제목, 내용 수정
     * */
    public Long teamPostUpdate(TeamPostUpdateRequestDto teamPostUpdateRequestDto, Long teamId) {
        String title = teamPostUpdateRequestDto.getTitle();
        String content = teamPostUpdateRequestDto.getContent();

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NullPointerException("Team not found"));

        team.toTeamByTeamPost(title, content);

        Team returnTeam = teamRepository.save(team);

        return returnTeam.getId();
    }
    /**
     * 팀 댓글 추가
     * */
    @Transactional
    public Long addTeamComment(TeamCommentAddRequestDto dto, Long teamId) {
        // NOTE 팀 검색
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NullPointerException("Team not found"));

        // NOTE 접속한 사용자
        Long user_id = 1L;
        User user = userRepository.findById(user_id)
                .orElseThrow(() -> new NullPointerException("User not found"));
        // NOTE 댓글 등록
        if (dto.getParentId() == 0){
        TeamComment teamComment = new TeamComment(dto.getContent(), user, team);
        teamCommentRepository.save(teamComment);
        // NOTE 대댓글 등록
        }else if (dto.getParentId() != 0){
            TeamComment teamComment = teamCommentRepository.findTeamCommentByTeamIdAndId(teamId, dto.getParentId());
            // NOTE 대댓글의 댓글은 불가
            if (teamComment.getParent() != null){
                throw new NullPointerException("대대댓글은 불가");
            }
            TeamComment teamComment2 = new TeamComment(dto.getContent(), user, team);
            List<TeamComment> teamComments = new ArrayList<>();
            teamComment2.setParent(teamComment);
            teamComments.add(teamComment);
            teamComment2.setChildren(teamComments);
            teamCommentRepository.save(teamComment2);
        }
        return teamId;
    }
    /**
     *  팀 추천 (조회수순으로 가져와서 섞음)
     * */
    public List<TeamDetailRecommendReaponseDto> getTeamRecommend(Long teamId) {
        List<Team> teams = teamRepository.findTop5ByStatusOrderByViewCount(Status.DISPLAYED)
                .orElseThrow(() -> new NullPointerException("not found team"));

        Collections.shuffle(teams);

        return teams.stream()
                .map(TeamDetailRecommendReaponseDto::new)
                .collect(Collectors.toList());
    }
    /**
     *  팀 좋아요 기능
     * */
    @Transactional
    public void teamLike(Long teamId, String authHeader) {
        Long userId = tokenProvider.getUserIdFromToken(authHeader);

        TeamLike teamLike = teamLikeRepository.countTeamLikesByTeamAndUser(teamId, userId)
                .orElseGet(() -> new TeamLike());

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new NullPointerException("not found team"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NullPointerException("not found user"));

        // NOTE 이미 좋아요 있음 (좋아요 취소)
        if (teamLike.getId() != null){
            teamLikeRepository.deleteById(teamLike.getId());
        // NOTE 좋아요 등록 (좋아요 등록)
        }else {
            teamLikeRepository.save(new TeamLike(team, user));
        }
    }
}
