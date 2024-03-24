package hanium.cocam.user;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@Getter
@AllArgsConstructor
@Table(name = "TB_USER")
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userNo; // 시퀀스 번호 PK
    private String userId;
    private String password;
    private String userName;
    private String userEmail;
    private String userNickName;
    private String userSex;
    private String userPhone;
    private int userAge;
    @Enumerated(EnumType.STRING)
    private UserType userType;
    private String mentorProfile; // 멘토 프로필 사진
    private String mentorUniv; // 멘토 소속 대학
    private String mentorClassNum; // 멘토 학번(20학번, 21학번 ...)
    private String mentorMajor; // 멘토 학과
    private String mentorIntro; // 멘토 소개글 (자유 형식)
    private String mentorMbti; // 멘토 Mbti
}