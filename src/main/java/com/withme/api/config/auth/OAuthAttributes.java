package com.withme.api.config.auth;

import com.withme.api.domain.user.User;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.utility.RandomString;

import java.util.Map;

@Slf4j
@ToString
@Getter
public class OAuthAttributes {
    private Map<String, Object> attributes;
    private String nameAttributeKey;
    private String nickname;
    private String email;
    private String userImage;

    @Builder
    public OAuthAttributes(Map<String, Object> attributes, String nameAttributeKey, String name, String email, String picture){
        this.attributes = attributes;
        this.nameAttributeKey = nameAttributeKey;
        this.nickname = name;
        this.email = email;
        this.userImage = picture;
    }

    //OAuth2User에서 반환하는 사용자 정보는 Map이기 때문에 값 하나하나를 변환해야 한다.
    public static OAuthAttributes of(String registrationId, String userNameAttributeName, Map<String, Object> attributes){
        log.debug("registrationId : " + registrationId);

        if("naver".equals(registrationId)){
            return ofNaver("id", attributes);
        }
        return ofGoogle(userNameAttributeName, attributes);
    }

    private static OAuthAttributes ofGoogle(String userNameAttributeName, Map<String, Object> attributes){
        log.debug("OAuthAttributes_ofGoogle");
        return OAuthAttributes.builder()
                .name((String) attributes.get("name"))
                .email((String) attributes.get("email"))
                .picture((String) attributes.get("picture"))
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    private static OAuthAttributes ofNaver(String userNameAttributeName, Map<String, Object> attributes){
        log.debug("OAuthAttributes_ofNaver");
        Map<String, Object> response = (Map<String, Object>)attributes.get("response");
        return OAuthAttributes.builder()
                .name((String) response.get("name"))
                .email((String) response.get("email"))
                .picture((String) response.get("profile_image"))
                .attributes(response)
                .nameAttributeKey(userNameAttributeName)
                .build();
    }

    // TODO: 2022/06/24 1. 닉네임 중복 발생 가능. unique하게 처리할 요건 필요, 2. 비밀번호 랜덤하게 처리할 요건 필요

    //OAuthAttributes에서 엔티티를 생성하는 시점은 처음 가입 시
    public User toEntity(String registrationId){
        return User.builder()
                .email(email)
                .password(RandomString.make(30))
                .nickname(nickname)
                .activated(true)
                .userImage(userImage)
                .role("ROLE_USER")
                .joinRoot(registrationId)
                .build();
    }
}
