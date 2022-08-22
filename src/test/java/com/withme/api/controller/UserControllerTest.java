package com.withme.api.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.withme.api.controller.dto.JoinRequestDto;
import com.withme.api.controller.dto.UserUpdateRequestDto;
import com.withme.api.domain.team.Status;
import com.withme.api.domain.team.Team;
import com.withme.api.domain.team.TeamCategory;
import com.withme.api.domain.user.User;
import com.withme.api.domain.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("local")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserController userController;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    private final String setupEmail = "set@up.com";
    private final String setupNick = "setNick";

    @BeforeEach
    public void setup(){
        this.mvc = MockMvcBuilders
                .webAppContextSetup(this.context)
                .apply(springSecurity())
                .build();

        JoinRequestDto dto = JoinRequestDto.builder()
                .email(this.setupEmail)
                .password("1234qwer%T")
                .nickname(this.setupNick)
                .build();

        userController.createUser(dto);
    }

    @AfterEach
    public void tearDown() {
        userRepository.findAll().forEach(user -> userRepository.delete(user));
    }

    @Test
    public void 회원가입_성공() throws Exception{
        //given
        String email = "joinTest@withme.com";
        String password = "1234qwer%T";
        String nickname = "vV위드미Vv";

        String apiUrl = "/api/v1/join";

        JoinRequestDto dto = JoinRequestDto.builder()
                .email(email)
                .password(password)
                .nickname(nickname)
                .build();

        String url = "http://localhost:" + port + apiUrl;

        //when
        mvc.perform(post(url)   //생성된 MockMvc를 통해 API를 테스트
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"email\":\"" + dto.getEmail() + "\"" +
                                ",\"password\":\"1234qwer%T\"" +
                                ",\"nickname\":\"" + dto.getNickname() + "\"" +
                                "}"
                        ))

        //then
                .andExpect(status().isCreated());

        assertThat(userRepository.findByEmailAndPasswordIsNotNull(email)
                .map(User::getNickname)).isEqualTo(Optional.of(nickname));
    }

    @Test
    public void 회원가입_실패_유효성_부적합() throws Exception{
        //given
        String email = "joinTest";
        String password = "12345";
        String nickname = "v";

        String apiUrl = "/api/v1/join";

        JoinRequestDto dto = JoinRequestDto.builder()
                .email(email)
                .password(password)
                .nickname(nickname)
                .build();

        String url = "http://localhost:" + port + apiUrl;

        //when
        mvc.perform(post(url)   //생성된 MockMvc를 통해 API를 테스트
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"email\":\"" + dto.getEmail() + "\"" +
                                ",\"password\":\"12345\"" +
                                ",\"nickname\":\"" + dto.getNickname() + "\"" +
                                "}"
                        ))

                //then
                .andExpect(status().is4xxClientError())
//                .andExpect(content().json("{\"message\": \"Validation Failed\"}"));
                .andExpect(jsonPath("$.message").value("Validation Failed"));


    }


    @Test
    public void 회원가입_실패_이메일_중복() throws Exception{
        //given

        String email = this.setupEmail;
        String password = "1234qwer%T";
        String nickname = "vV위드미VvV";

        String apiUrl = "/api/v1/join";

        JoinRequestDto dto = JoinRequestDto.builder()
                .email(email)
                .password(password)
                .nickname(nickname)
                .build();

        String url = "http://localhost:" + port + apiUrl;

        //when
        mvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"email\":\"" + dto.getEmail() + "\"" +
                                ",\"password\":\"1234qwer%T\"" +
                                ",\"nickname\":\"" + dto.getNickname() + "\"" +
                                "}"
                        ))

                //then
                .andExpect(status().is4xxClientError())
//                .andExpect(content().json("{\"message\": \"Email Duplicated\"}"));
                .andExpect(jsonPath("$.message").value("Email Duplicated"));


    }


    @Test
    public void 회원가입_실패_닉네임_중복() throws Exception{
        //given
        String email = "joinTest1@withme.com";
        String password = "1234qwer%T";
        String nickname = this.setupNick;

        String apiUrl = "/api/v1/join";

        JoinRequestDto dto = JoinRequestDto.builder()
                .email(email)
                .password(password)
                .nickname(nickname)
                .build();

        String url = "http://localhost:" + port + apiUrl;

        //when
        mvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"email\":\"" + dto.getEmail() + "\"" +
                                ",\"password\":\"1234qwer%T\"" +
                                ",\"nickname\":\"" + dto.getNickname() + "\"" +
                                "}"
                        ))

                //then
                .andExpect(status().is4xxClientError())
//                .andExpect(content().json("{\"message\": \"Nickname Duplicated\"}"));
                .andExpect(jsonPath("$.message").value("Nickname Duplicated"));



    }


    @Test
    @WithMockUser(roles = "USER")
    public void 닉네임변경_성공() throws Exception{
        //given
        Long id = userRepository.findAll().get(0).getId();
        String nicknameToBeChanged = "vV위드미Vv";

        String apiUrl = "/api/v1/user/nickname/"+id;

        UserUpdateRequestDto dto = UserUpdateRequestDto.builder()
                .nickname(nicknameToBeChanged)
                .build();

        String url = "http://localhost:" + port + apiUrl;

        //when
        mvc.perform(put(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(dto))
                )

                //then
                .andExpect(status().isOk());

        assertThat(userRepository.findById(id)
                .map(User::getNickname)).isEqualTo(Optional.of(nicknameToBeChanged));
    }

    @Test
    @WithMockUser(roles = "USER")
    public void 닉네임변경_실패_중복() throws Exception{
        //given
        Long id = userRepository.findAll().get(0).getId();
        String nicknameToBeChanged = this.setupNick;

        String apiUrl = "/api/v1/user/nickname/"+id;

        UserUpdateRequestDto dto = UserUpdateRequestDto.builder()
                .nickname(nicknameToBeChanged)
                .build();

        String url = "http://localhost:" + port + apiUrl;

        //when
        mvc.perform(put(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(dto))
                )

                //then
                .andExpect(status().is4xxClientError())
//                .andExpect(content().json("{\"message\": \"Nickname Duplicated\"}"));
                .andExpect(jsonPath("$.message").value("Nickname Duplicated"));
    }

    @Test
    @WithMockUser(roles = "USER")
    public void 닉네임변경_실패_유효성_부적합() throws Exception{
        //given
        Long id = 1L;
        String nicknameToBeChanged = "h";

        String apiUrl = "/api/v1/user/nickname/"+id;

        UserUpdateRequestDto dto = UserUpdateRequestDto.builder()
                .nickname(nicknameToBeChanged)
                .build();

        String url = "http://localhost:" + port + apiUrl;

        //when
        mvc.perform(put(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(dto))
                )

                //then
                .andExpect(status().is4xxClientError())
//                .andExpect(content().json("{\"message\": \"Validation Failed\"}"));
                .andExpect(jsonPath("$.message").value("Validation Failed"));

    }

    @Test
    @WithMockUser(roles = "USER")
    public void 닉네임변경_실패_id없음() throws Exception{
        //given
        Long id = 654356L;
        String nicknameToBeChanged = "nnname";

        String apiUrl = "/api/v1/user/nickname/"+id;

        UserUpdateRequestDto dto = UserUpdateRequestDto.builder()
                .nickname(nicknameToBeChanged)
                .build();

        String url = "http://localhost:" + port + apiUrl;

        //when
        mvc.perform(put(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(dto))
                )

                //then
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.message").value("User Not Found. id : " + id));

    }

    @Test
    @WithMockUser(roles = "USER")
    @Transactional
    public void 마이페이지조회_성공() throws Exception {
        //given
        User user = userRepository.findAll().get(0);
        Long id = user.getId();

        Team team1 = Team.builder()
                .teamName("TestTeamName1")
                .teamCategory(TeamCategory.STUDY)
                .teamDesc("This is TestTeam Description1")
                .status(Status.DISPLAYED)
                .build();

        Team team2 = Team.builder()
                .teamName("TestTeamName2")
                .teamCategory(TeamCategory.STUDY)
                .teamDesc("This is TestTeam Description2")
                .status(Status.HIDDEN)
                .build();

        user.joinTeam(team1);
        user.joinTeam(team2);

        String apiUrl = "/api/v1/user/mypage/"+id;
        String url = "http://localhost:" + port + apiUrl;

        String expectedNickname = "$.[?(@.nickname == '%s')]";
        String expectedUserImage = "$.[?(@.userImage == '%s')]";
        String expectedTeamName = "$..teamList[?(@.teamName == '%s')]";
        String expectedTeamStatus = "$..teamList[?(@.status == '%s')]";
        String expectedTeamDesc = "$..teamList[?(@.teamDesc == '%s')]";

        //when
        mvc.perform(get(url))

            //then
            .andExpect(status().isOk())
            .andExpect(jsonPath(expectedNickname, user.getNickname()).exists())
            .andExpect(jsonPath(expectedUserImage, user.getUserImage()).exists())
            .andExpect(jsonPath(expectedTeamName, team1.getTeamName()).exists())
            .andExpect(jsonPath(expectedTeamName, team2.getTeamName()).exists())
            .andExpect(jsonPath(expectedTeamStatus, team1.getStatus()).exists())
            .andExpect(jsonPath(expectedTeamStatus, team2.getStatus()).exists())
            .andExpect(jsonPath(expectedTeamDesc, team1.getTeamDesc()).exists())
            .andExpect(jsonPath(expectedTeamDesc, team2.getTeamDesc()).exists());
    }

}
