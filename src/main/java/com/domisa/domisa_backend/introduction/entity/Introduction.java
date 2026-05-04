package com.domisa.domisa_backend.introduction.entity;

import com.domisa.domisa_backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "introductions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Introduction {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "q1", columnDefinition = "TEXT")
	private String q1;

	@Column(name = "q2", columnDefinition = "TEXT")
	private String q2;

	@Column(name = "q3", columnDefinition = "TEXT")
	private String q3;

	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	@JoinColumn(name = "introducer_id", nullable = true)
	private User introducer;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "participant_id", unique = true)
	private User participant;

	@Column(name = "link_code")
	private String linkCode;

	private Introduction(String q1, String q2, String q3, User introducer, String linkCode) {
		this.q1 = q1;
		this.q2 = q2;
		this.q3 = q3;
		this.introducer = introducer;
		this.linkCode = linkCode;
	}

	public static Introduction create(String q1, String q2, String q3, User introducer, String linkCode) {
		return new Introduction(q1, q2, q3, introducer, linkCode);
	}

	public void assignParticipant(User participant) {
		this.participant = participant;
		participant.setIntroduction(this);
		participant.setHasIntroduction(true);
	}

	public void clearParticipant() {
		if (this.participant == null) {
			return;
		}
		User currentParticipant = this.participant;
		this.participant = null;
		currentParticipant.setIntroduction(null);
		currentParticipant.setHasIntroduction(false);
	}
}
