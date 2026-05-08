package com.domisa.domisa_backend.dating.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.domisa.domisa_backend.card.entity.Card;
import com.domisa.domisa_backend.dating.dto.DatingProfileDetailRequest;
import com.domisa.domisa_backend.dating.dto.DatingProfileResponse;
import com.domisa.domisa_backend.global.s3.service.S3ObjectUrlService;
import com.domisa.domisa_backend.introduction.entity.Introduction;
import com.domisa.domisa_backend.introduction.repository.IntroductionRepository;
import com.domisa.domisa_backend.notification.service.NotificationService;
import com.domisa.domisa_backend.notification.type.NotificationType;
import com.domisa.domisa_backend.payment.repository.CookieTransactionRepository;
import com.domisa.domisa_backend.user.entity.User;
import com.domisa.domisa_backend.user.repository.UserRepository;
import com.domisa.domisa_backend.user.type.AnimalProfile;
import com.domisa.domisa_backend.user.type.ContactType;
import com.domisa.domisa_backend.user.type.Mbti;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DatingServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private IntroductionRepository introductionRepository;

	@Mock
	private S3ObjectUrlService s3ObjectUrlService;

	@Mock
	private CookieTransactionRepository cookieTransactionRepository;

	@Mock
	private NotificationService notificationService;

	@InjectMocks
	private DatingService datingService;

	@Test
	void getDatingProfileFanViewReturnsBlurredTextAndFreeLikeRemaining() {
		User requester = createUser(1L, "REQ001");
		requester.setMyFans(List.of(2L));
		requester.setFreeLikeCount(2);

		User target = createCompletedTargetUser();
		String q3 = target.getIntroduction().getQ3();
		String idealType = target.getCard().getIdealType();

		when(userRepository.findWithProfileImageById(1L)).thenReturn(Optional.of(requester));
		when(userRepository.findDatingProfileByPublicId("TGT001")).thenReturn(Optional.of(target));
		when(s3ObjectUrlService.getThumbnailBlurPresignedUrl(isNull())).thenReturn("https://example.com/blur.jpg");

		DatingProfileResponse response = datingService.getDatingProfile(
			requester,
			"TGT001",
			new DatingProfileDetailRequest(DatingProfileDetailRequest.ViewType.FAN)
		);

		assertThat(response.publicId()).isEqualTo("TGT001");
		assertThat(response.nickName()).isEqualTo("수빈");
		assertThat(response.age()).isEqualTo(Year.now().getValue() - 2004 + 1);
		assertThat(response.animalProfile()).isEqualTo(AnimalProfile.CAT);
		assertThat(response.profile()).isEqualTo("https://example.com/blur.jpg");
		assertThat(response.q1()).isEqualTo("귀여워요");
		assertThat(response.q2()).isEqualTo("길에서 춤춰요");
		assertThat(response.q3()).isNull();
		assertThat(response.q3Length()).isEqualTo(q3.length());
		assertThat(response.datingStyle()).isEqualTo("산책 같이 하고 싶어요");
		assertThat(response.idealType()).isNull();
		assertThat(response.idealTypeLength()).isEqualTo(idealType.length());
		assertThat(response.mbti()).isEqualTo(Mbti.INFJ);
		assertThat(response.contact()).isNull();
		assertThat(response.isBlurred()).isTrue();
		assertThat(response.isPaidUnblur()).isFalse();
		assertThat(response.hasSentLike()).isFalse();
		assertThat(response.hasReceivedLike()).isTrue();
		assertThat(response.isMatched()).isFalse();
		assertThat(response.freeLikeRemaining()).isEqualTo(2);
	}

	@Test
	void sendLikeAddsMyTypesAndTargetFansWithoutChangingCurrentShows() {
		User requester = createUser(1L, "REQ001");
		requester.setFreeLikeCount(1);
		requester.setMyFans(new ArrayList<>(List.of(2L)));
		requester.setNowShows(new ArrayList<>(List.of(2L)));
		User target = createUser(2L, "TGT001");
		target.setMyTypes(new ArrayList<>(List.of(1L)));

		when(userRepository.findWithProfileImageById(1L)).thenReturn(Optional.of(requester));
		when(userRepository.findByPublicId("TGT001")).thenReturn(Optional.of(target));

		datingService.sendLike(requester, "TGT001");

		assertThat(requester.getFreeLikeCount()).isZero();
		assertThat(requester.getMyTypes()).containsExactly(2L);
		assertThat(requester.getMyBlurs()).isNull();
		assertThat(requester.getMyMatches()).isNull();
		assertThat(requester.getNowShows()).containsExactly(2L);
		assertThat(target.getMyFans()).containsExactly(1L);
		assertThat(target.getMyTypes()).containsExactly(1L);
		assertThat(target.getMyMatches()).isNull();
		verify(notificationService).createNotification(NotificationType.LIKE, 2L, 1L);
	}

	@Test
	void matchReceivedLikeRequiresUnblurAndMovesReceivedLikeToMatches() {
		User requester = createUser(1L, "REQ001");
		requester.setMyFans(new ArrayList<>(List.of(2L)));
		requester.setMyBlurs(new ArrayList<>(List.of(2L)));
		User target = createUser(2L, "TGT001");
		target.setMyTypes(new ArrayList<>(List.of(1L)));

		when(userRepository.findWithProfileImageById(1L)).thenReturn(Optional.of(requester));
		when(userRepository.findByPublicId("TGT001")).thenReturn(Optional.of(target));

		datingService.matchReceivedLike(requester, "TGT001");

		assertThat(requester.getMyFans()).isEmpty();
		assertThat(requester.getMyBlurs()).containsExactly(2L);
		assertThat(requester.getMyMatches()).containsExactly(2L);
		assertThat(target.getMyTypes()).isEmpty();
		assertThat(target.getMyMatches()).containsExactly(1L);
		verify(notificationService).createNotification(NotificationType.MATCH, 1L, 2L);
		verify(notificationService).createNotification(NotificationType.MATCH, 2L, 1L);
	}

	@Test
	void getDatingProfileUnblurredButNotMatchedKeepsContactHidden() {
		User requester = createUser(1L, "REQ001");
		requester.setMyBlurs(List.of(2L));
		requester.setFreeLikeCount(1);
		User target = createCompletedTargetUser();

		when(userRepository.findWithProfileImageById(1L)).thenReturn(Optional.of(requester));
		when(userRepository.findDatingProfileByPublicId("TGT001")).thenReturn(Optional.of(target));
		when(s3ObjectUrlService.getThumbnailPresignedUrl(isNull())).thenReturn("https://example.com/profile.jpg");

		DatingProfileResponse response = datingService.getDatingProfile(
			requester,
			"TGT001",
			new DatingProfileDetailRequest(DatingProfileDetailRequest.ViewType.FAN)
		);

		assertThat(response.profile()).isEqualTo("https://example.com/profile.jpg");
		assertThat(response.q3()).isEqualTo("곰같은 남자랑 맞을 것 같아요");
		assertThat(response.idealType()).isEqualTo("곰같은 남자");
		assertThat(response.contact()).isNull();
		assertThat(response.isBlurred()).isFalse();
		assertThat(response.isPaidUnblur()).isTrue();
		assertThat(response.isMatched()).isFalse();
	}

	@Test
	void shuffleResetsFreeLikeCountToThree() {
		User requester = createUser(1L, "REQ001");
		requester.setGender(true);
		requester.setCookies(2L);
		requester.setFreeLikeCount(0);
		requester.setNowShows(new ArrayList<>(List.of(9L)));
		completeProfile(requester);

		when(userRepository.findWithProfileImageById(1L)).thenReturn(Optional.of(requester));
		when(userRepository.findRandomOppositeGenderUserIds(1L, true, 8))
			.thenReturn(List.of(2L, 3L));

		datingService.shuffle(requester);

		assertThat(requester.getNowShows()).containsExactly(2L, 3L);
		assertThat(requester.getFreeLikeCount()).isEqualTo(3);
		assertThat(requester.getCookies()).isZero();
	}

	@Test
	void getDatingProfileMatchedViewReturnsOpenProfileAndContact() {
		User requester = createUser(1L, "REQ001");
		requester.setMyMatches(List.of(2L));
		requester.setMyBlurs(List.of(2L));
		requester.setFreeLikeCount(1);

		User target = createCompletedTargetUser();

		when(userRepository.findWithProfileImageById(1L)).thenReturn(Optional.of(requester));
		when(userRepository.findDatingProfileByPublicId("TGT001")).thenReturn(Optional.of(target));
		when(s3ObjectUrlService.getThumbnailPresignedUrl(isNull())).thenReturn("https://example.com/profile.jpg");

		DatingProfileResponse response = datingService.getDatingProfile(
			requester,
			"TGT001",
			new DatingProfileDetailRequest(DatingProfileDetailRequest.ViewType.FAN)
		);

		assertThat(response.profile()).isEqualTo("https://example.com/profile.jpg");
		assertThat(response.q3()).isEqualTo("곰같은 남자랑 맞을 것 같아요");
		assertThat(response.q3Length()).isNull();
		assertThat(response.idealType()).isEqualTo("곰같은 남자");
		assertThat(response.idealTypeLength()).isNull();
		assertThat(response.contact().type()).isEqualTo(ContactType.KAKAO);
		assertThat(response.contact().content()).isEqualTo("subin1234");
		assertThat(response.isBlurred()).isFalse();
		assertThat(response.isPaidUnblur()).isFalse();
		assertThat(response.isMatched()).isTrue();
	}

	private User createCompletedTargetUser() {
		User user = createUser(2L, "TGT001");
		user.setNickname("수빈");
		user.setBirthYear(2004L);
		user.setGender(false);
		user.setAnimalProfile(AnimalProfile.CAT);
		user.setContactType(ContactType.KAKAO);
		user.setContact("subin1234");

		Introduction introduction = Introduction.create(
			"귀여워요",
			"길에서 춤춰요",
			"곰같은 남자랑 맞을 것 같아요",
			null,
			"LINK001"
		);
		introduction.assignParticipant(user);

		Card card = Card.create(
			user,
			Mbti.INFJ,
			"산책 같이 하고 싶어요",
			"곰같은 남자",
			null
		);
		user.setCard(card);
		user.setIsRegistered(true);
		user.setIsProfileCompleted(true);
		return user;
	}

	private void completeProfile(User user) {
		user.setIsRegistered(true);
		Introduction.create("q1", "q2", "q3", null, "LINK" + user.getId()).assignParticipant(user);
		user.setCard(Card.create(user, Mbti.INFJ, "style", "ideal", null));
		user.setIsProfileCompleted(true);
	}

	private User createUser(Long id, String publicId) {
		User user = User.create(id);
		user.setId(id);
		user.setPublicId(publicId);
		return user;
	}
}
