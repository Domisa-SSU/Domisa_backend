package com.domisa.domisa_backend.introduction.entity;

import com.domisa.domisa_backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "introducer_id")
	private User introducer;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "participant_id", nullable = false, unique = true)
	private User participant;

	@Column(name = "link_code")
	private String linkCode;
}
