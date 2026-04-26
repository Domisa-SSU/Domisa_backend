package com.domisa.domisa_backend.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

	@Column(length = 1024)
	private String profileImageObjectKey;

	@Column(nullable = false)
	private Long profileImageSequence = 0L;

	private User(String kakaoId) {
		this.kakaoId = kakaoId;
		this.profileImageSequence = 0L;
	}

	public static User create(String kakaoId) {
		return new User(kakaoId);
	}

	public void registerProfile(
		String nickname,
		Boolean gender,
		String birthYear,
		String animalProfile,
		String contact,
		String inviteCode
	) {
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

	public boolean hasProfileImage() {
		return profileImageObjectKey != null && !profileImageObjectKey.isBlank();
	}

	public Integer getAge() {
		if (birthYear == null) {
			return null;
		}
		int currentYear = java.time.Year.now().getValue();
		return currentYear - Integer.parseInt(birthYear) + 1;
	}

	public String getGenderDisplay() {
		if (gender == null) {
			return null;
		}
		return gender ? "남" : "여";
	}
}
