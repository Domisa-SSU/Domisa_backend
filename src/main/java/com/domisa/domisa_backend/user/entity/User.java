package com.domisa.domisa_backend.user.entity;

import com.domisa.domisa_backend.card.entity.Card;
import com.domisa.domisa_backend.introduction.entity.Introduction;
import com.domisa.domisa_backend.profileimage.entity.ProfileImage;
import com.domisa.domisa_backend.user.type.AnimalProfile;
import com.domisa.domisa_backend.user.type.ContactType;
import com.domisa.domisa_backend.user.util.UserPublicIdGenerator;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.time.Year;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "public_id", nullable = false, unique = true, updatable = false, length = 16)
	private String publicId;

	@Column(name = "name", length = 20)
	private String name;

	@Column(name = "nickname", length = 20)
	private String nickname;

	@Column(name = "gender")
	private Boolean gender;

	@Column(name = "birth_year")
	private Long birthYear;

	@Column(name = "animal_profile", length = 20)
	@Enumerated(EnumType.STRING)
	private AnimalProfile animalProfile;

	@Column(name = "profile_key", length = 200)
	private String profile_key;

	@Column(name = "cookies", nullable = false)
	private Long cookies = 0L;

	@Column(name = "contact", length = 30)
	private String contact;

	@Column(name = "contact_type", length = 20)
	@Enumerated(EnumType.STRING)
	private ContactType contactType;

	@Column(name = "invite_code", length = 20)
	private String inviteCode;

	@OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
	private Card card;

	@OneToOne(mappedBy = "participant", fetch = FetchType.LAZY)
	private Introduction introduction;

	@OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
	private ProfileImage profileImage;

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "user_my_blurs", joinColumns = @JoinColumn(name = "user_id"))
	@Column(name = "target_user_id")
	private List<Long> myBlurs;

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "user_my_fans", joinColumns = @JoinColumn(name = "user_id"))
	@Column(name = "target_user_id")
	private List<Long> myFans;

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "user_my_types", joinColumns = @JoinColumn(name = "user_id"))
	@Column(name = "target_user_id")
	private List<Long> myTypes;

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "user_now_shows", joinColumns = @JoinColumn(name = "user_id"))
	@Column(name = "target_user_id")
	private List<Long> nowShows;

	@Column(name = "refresh_at")
	private LocalDateTime refreshAt;

	@Column(name = "kakao_id", nullable = false, unique = true)
	private Long kakaoId;

	@Column(name = "is_registered", nullable = false)
	private Boolean isRegistered = false;

	@Column(name = "profile_image_sequence", nullable = false)
	private Long profileImageSequence = 0L;

	@Column(name = "profile_image_object_key", length = 1024)
	private String profileImageObjectKey;

	@Column(name = "has_introduction", nullable = false)
	private Boolean hasIntroduction = false;

	@Column(name = "is_profile_completed", nullable = false)
	private Boolean isProfileCompleted = false;
  
	private User(Long kakaoId) {
		this.kakaoId = kakaoId;
	}

	public static User create(Long kakaoId) {
		return new User(kakaoId);
	}

	@PrePersist
	void assignPublicIdIfAbsent() {
		if (publicId == null || publicId.isBlank()) {
			publicId = UserPublicIdGenerator.generate();
		}
	}

	public boolean hasProfileImage() {
		return profileImage != null && profileImage.hasOriginKey();
	}

	public Integer getAge() {
		if (birthYear == null) {
			return null;
		}
		return Year.now().getValue() - birthYear.intValue() + 1;
	}

	public String getGenderDisplay() {
		if (gender == null) {
			return null;
		}
		return gender ? "남" : "여";
	}

	public boolean hasCard() {
		return Boolean.TRUE.equals(isProfileCompleted);
	}

	public boolean hasIntroduction() {
		return Boolean.TRUE.equals(hasIntroduction);
	}
}
