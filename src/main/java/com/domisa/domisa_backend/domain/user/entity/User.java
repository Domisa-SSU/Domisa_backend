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
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 50)
	@Setter
	private String name;

	@Column(nullable = false, length = 50)
	@Setter
	private String nickname;

	@Column(length = 1024)
	@Setter
	private String profileImageObjectKey;

	@Column(nullable = false)
	@Setter
	private Long profileImageSequence = 0L;

	public User(String name, String nickname) {
		this.name = name;
		this.nickname = nickname;
		this.profileImageSequence = 0L;
	}

	public boolean hasProfileImage() {
		return profileImageObjectKey != null && !profileImageObjectKey.isBlank();
	}
}
