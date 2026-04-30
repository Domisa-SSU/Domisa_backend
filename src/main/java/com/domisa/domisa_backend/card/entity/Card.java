package com.domisa.domisa_backend.card.entity;

import com.domisa.domisa_backend.user.entity.User;
import com.domisa.domisa_backend.user.type.Mbti;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "cards")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Card {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private User user;

	@Column(name = "ideal_type")
	private String idealType;

	@Column(name = "dating_style")
	private String datingStyle;

	@Column(name = "mbti", length = 20)
	@Enumerated(EnumType.STRING)
	private Mbti mbti;

	// S3 ObjectKey 저징
	@Column(name = "image_key", length = 1024)
	private String imageKey;

	public static Card create(User user, Mbti mbti, String datingStyle, String idealType, String imageKey) {
		Card card = new Card();
		card.user = user;
		card.mbti = mbti;
		card.datingStyle = datingStyle;
		card.idealType = idealType;
		card.imageKey = imageKey;
		return card;
	}

	public void update(Mbti mbti, String datingStyle, String idealType, String imageKey) {
		this.mbti = mbti;
		this.datingStyle = datingStyle;
		this.idealType = idealType;
		this.imageKey = imageKey;
	}
}
