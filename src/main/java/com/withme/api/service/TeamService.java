package com.withme.api.service;

import com.withme.api.controller.dto.CreateTeamRequestDto;
import com.withme.api.controller.dto.TeamListResponseMapping;
import com.withme.api.controller.dto.TeamNoticeCreateRequestDto;
import com.withme.api.controller.dto.TeamSearchDto;
import com.withme.api.domain.skill.Skill;
import com.withme.api.domain.skill.SkillName;
import com.withme.api.domain.team.Status;
import com.withme.api.domain.team.Team;
import com.withme.api.domain.team.TeamCategory;
import com.withme.api.domain.team.TeamRepository;
import com.withme.api.domain.teamNotice.TeamNotice;
import com.withme.api.domain.teamNotice.TeamNoticeRepository;
import com.withme.api.domain.teamSkill.TeamSkill;
import com.withme.api.domain.teamSkill.TeamSkillRepository;
import com.withme.api.domain.teamUser.MemberType;
import com.withme.api.domain.teamUser.TeamUser;
import com.withme.api.domain.user.User;
import com.withme.api.domain.user.UserRepository;
import com.withme.api.jwt.TokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;

    private final TokenProvider tokenProvider;
    private final TeamNoticeRepository teamNoticeRepository;


    private final TeamSkillRepository teamSkillRepository;
    /**
     * 팀 리스트 조회
     * */
    public Map<String, Object> selectTeamList(Map<String, Object> params){
        try{
            Map<String, Object> result = new HashMap<>();
//            List<TeamListResponseMapping> teamsList = teamRepository.findTeamsByOrderById()
//                    .orElseThrow(()-> new NullPointerException("팀 조회 중 오류"));

            // NOTE 팀 카운트 조회
            int countTeamBy = teamRepository.countTeamBy();
            result.put("teamCount", countTeamBy);
//            result.put("teamsList", teamsList);

            return result;
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }
    @Transactional
    public List<TeamListResponseMapping> getTeamList(TeamSearchDto map) throws Exception {

        List<Skill> skillList = new ArrayList<>();
        List<TeamListResponseMapping> teamList = new ArrayList<>();
        List<TeamSkill> teamSkillsParams = new ArrayList<>();
        List<List<TeamSkill>> teamSkillsParamsList = new ArrayList<>();
        // NOTE 검색 조건 사용을 위해 TeamSkill 조회
        List<TeamSkill> teamSkills = teamSkillRepository.findAll();
        // NOTE 스킬 입력
        for (SkillName names: map.getSkills()){
            skillList.add(Skill.builder().skillName(names).build());
        }

        // NOTE 검색 조건 걸러냄
        for (Skill skill : skillList) {
            teamSkillsParams = teamSkills.stream()
                    .filter(teamSkill -> {
                            return teamSkill.getSkill().getSkillName().equals(skill.getSkillName());
                    }).collect(Collectors.toList());
            teamSkillsParamsList.add(teamSkillsParams);
        }
        // NOTE 검색 조건을 위해 리스트를 합침
        List<TeamSkill> params = teamSkillsParamsList.stream()
                .flatMap(x -> x.stream())
                .collect(Collectors.toList());

        if (teamSkills.size() <= 0){
            // NOTE 내림 차순
            if (map.getSort() == 0){
                teamList = teamRepository.findAllByStatusOrderByCreatedTimeDesc(Status.DISPLAYED).orElseThrow(
                        () -> new NullPointerException("팀 조회 오류 (검색X)")
                );
            }else {
                teamList = teamRepository.findAllByStatusOrderByCreatedTimeAsc(Status.DISPLAYED).orElseThrow(
                        () -> new NullPointerException("팀 조회 오류 (검색X)")
                );
            }
        }else{
            if (map.getSort() == 0){
                teamList = teamRepository.findTeamsByTeamSkillsInAndStatusOrderByCreatedTimeDesc(params, Status.DISPLAYED)
                        .orElseThrow(
                                () -> new NullPointerException("팀 조회 오류 (검색O)")
                        );
            }else {
                teamList = teamRepository.findTeamsByTeamSkillsInAndStatusOrderByCreatedTimeAsc(params, Status.DISPLAYED)
                        .orElseThrow(
                                () -> new NullPointerException("팀 조회 오류 (검색O)")
                        );
            }
        }

        return teamList;

    }

    /**
     * 팀 등록
     * */
    @Transactional
    public int createTeamTest(CreateTeamRequestDto createTeamDto) {
        // NOTE 현재 접속한 유저 ID 구해서 적용필요
        Long user_idx = 1L;

        Team team = Team.builder()
                    .teamCategory(TeamCategory.PROJECT)
                    .teamDesc(createTeamDto.getDescription())
                    .teamName(createTeamDto.getName())
                    .status(Status.DISPLAYED)
                    .build();

            // NOTE 스킬 입력
            Skill skill = new Skill();
            TeamSkill teamSkill = new TeamSkill();
            for (String skillName : createTeamDto.getSkills()) {
                skill = Skill.builder()
                        .skillName(SkillName.valueOf(skillName))
                        .build();
                teamSkill = TeamSkill.builder()
                        .team(team)
                        .skill(skill)
                        .build();
                team.addTeamSkill(teamSkill);
            }


            User user = userRepository.findById(user_idx).orElseThrow(
                    ()-> new NullPointerException("존재하지않는 사용자"));

            log.info("team ::: " + user);
            // NOTE 팀등록
            TeamUser teamUser = TeamUser.builder()
                    .memberType(MemberType.LEADER)
                    .team(team)
                    .user(user)
                    .build();
            team.addTeamUser(teamUser);
            teamRepository.save(team);

            return 1;
    }


    /**
     * 팀 상세 정보 조회
     * */
    public void teamDetailInfo() {
        //TeamRepository.
    }

//    /**
//     * 팀 삭제
//     * */
//    public Map<String, Object> deleteTeam(Map<String, Object> params){
//        HashMap<String, Object> result = new HashMap<>();
//        try {
//            teamRepository.deleteById(((Number)params.get("team_idx")).longValue());
//            result.put("result", "success");
//            return result;
//        }catch (Exception e){
//            e.printStackTrace();
//            result.put("result", "Exception");
//            return result;
//        }
//    }
//

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

}
