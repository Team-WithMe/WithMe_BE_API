package com.withme.api.domain.user;

import com.withme.api.domain.BaseTimeEntity;
import com.withme.api.domain.teamUser.TeamUser;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;


@ToString
@Getter
@NoArgsConstructor
@Entity
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_idx")
    private Long id;

    @Column(unique = true)
    private String email;

    @Column
    private String password;

    @Column(unique = true, nullable = false)
    private String nickname;

    @Column
    private String userImage;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private String joinRoot;

    @Column
    private String nameAttributeValue;

    @OneToMany(mappedBy = "user")
    private List<TeamUser> userTeams = new ArrayList<>();


    @Builder
    public User(String email, String password, String nickname, String userImage, String role, String joinRoot, String nameAttributeValue) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.userImage = userImage;
        this.role = role;
        this.joinRoot = joinRoot;
        this.nameAttributeValue = nameAttributeValue;
    }

    public User update(String userImage) {
        this.userImage = userImage;

        return this;
    }
}