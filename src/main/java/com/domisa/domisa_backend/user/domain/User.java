package com.domisa.domisa_backend.user.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String kakaoId;

    @Column
    private String nickname;

    @Column
    private Boolean gender;

    @Column
    private String birthYear;

    @Column
    private String animalProfile;

    @Column
    private String contact;

    @Column
    private String inviteCode;

    @Column(nullable = false)
    private Boolean isRegistered = false;

    @Column(nullable = false)
    private Boolean hasIntroduction = false;

    @Column(nullable = false)
    private Boolean isProfileCompleted = false;

    @Column(nullable = false)
    private Integer cookieCount = 0;

    @Column
    private String referralCode;

    @Column
    private String profileImageUrl;

    protected User() {}

    private User(String kakaoId) {
        this.kakaoId = kakaoId;
    }

    public static User create(String kakaoId) {
        return new User(kakaoId);
    }

    public void registerProfile(String nickname, Boolean gender, String birthYear,
                                String animalProfile, String contact, String inviteCode) {
        this.nickname = nickname;
        this.gender = gender;
        this.birthYear = birthYear;
        this.animalProfile = animalProfile;
        this.contact = contact;
        this.inviteCode = inviteCode;
        this.isRegistered = true;
        this.isProfileCompleted = true;
    }

    public void completeIntroduction() {
        this.hasIntroduction = true;
    }

    public Integer getAge() {
        if (birthYear == null) return null;
        int currentYear = java.time.Year.now().getValue();
        return currentYear - Integer.parseInt(birthYear) + 1;
    }

    public String getGenderDisplay() {
        if (gender == null) return null;
        return gender ? "남" : "여";
    }

    public Long getId() { return id; }
    public String getKakaoId() { return kakaoId; }
    public String getNickname() { return nickname; }
    public Boolean getGender() { return gender; }
    public String getBirthYear() { return birthYear; }
    public String getAnimalProfile() { return animalProfile; }
    public String getContact() { return contact; }
    public Boolean getIsRegistered() { return isRegistered; }
    public Boolean getHasIntroduction() { return hasIntroduction; }
    public Boolean getIsProfileCompleted() { return isProfileCompleted; }
    public Integer getCookieCount() { return cookieCount; }
    public String getReferralCode() { return referralCode; }
    public String getProfileImageUrl() { return profileImageUrl; }
}
