package com.domisa.domisa_backend.dummy.service;

import com.domisa.domisa_backend.card.entity.Card;
import com.domisa.domisa_backend.card.repository.CardRepository;
import com.domisa.domisa_backend.introduction.entity.Introduction;
import com.domisa.domisa_backend.introduction.repository.IntroductionRepository;
import com.domisa.domisa_backend.profileimage.entity.ProfileImage;
import com.domisa.domisa_backend.profileimage.repository.ProfileImageRepository;
import com.domisa.domisa_backend.user.entity.User;
import com.domisa.domisa_backend.user.repository.UserRepository;
import com.domisa.domisa_backend.user.type.AnimalProfile;
import com.domisa.domisa_backend.user.type.ContactType;
import com.domisa.domisa_backend.user.type.Mbti;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Order(2)
@Component
@RequiredArgsConstructor
public class DummyCompletedUserInitializer implements ApplicationRunner {

	private static final long DUMMY_KAKAO_ID_START = 9_000_000_001L;
	private static final List<DummyCompletedUserSeed> SEEDS = List.of(
		new DummyCompletedUserSeed(
			1,
			"미소",
			false,
			1999L,
			AnimalProfile.CAT,
			Mbti.ENFJ,
			ContactType.KAKAO,
			"miso_001",
			"조용한 분위기를 좋아하지만 좋아하는 사람 앞에서는 밝게 웃는 편이에요.",
			"각자의 시간을 존중하면서도 자주 마음을 확인하는 연애를 선호해요.",
			"전시를 보거나 새로 생긴 디저트 가게를 찾아가는 걸 좋아해요.",
			"말보다는 행동으로 꾸준히 마음을 보여주는 편이에요.",
			"다정하고 자기 생각을 부드럽게 말할 수 있는 사람이 좋아요."
		),
		new DummyCompletedUserSeed(
			2,
			"서아",
			false,
			2000L,
			AnimalProfile.RABBIT,
			Mbti.INFP,
			ContactType.INSTAGRAM,
			"seoa_002",
			"새로운 사람을 만나는 걸 좋아하고 분위기를 밝게 만드는 편이에요.",
			"함께 웃을 일이 많고 서로에게 좋은 자극을 주는 연애를 좋아해요.",
			"주말에는 즉흥적으로 맛집이나 근교 나들이를 가는 걸 좋아해요.",
			"좋아하는 마음이 생기면 표현을 아끼지 않는 편이에요.",
			"긍정적이고 대화를 재미있게 이어갈 수 있는 사람이 좋아요."
		),
		new DummyCompletedUserSeed(
			3,
			"나린",
			false,
			2001L,
			AnimalProfile.DEER,
			Mbti.ISTJ,
			ContactType.KAKAO,
			"narin_003",
			"주변 사람을 잘 챙기고 따뜻한 말을 건네는 것을 좋아해요.",
			"편안하게 기대고 의지할 수 있는 차분한 관계를 만들고 싶어요.",
			"요리하거나 좋아하는 음악을 틀어두고 쉬는 시간을 좋아해요.",
			"상대의 이야기를 잘 듣고 작은 변화도 놓치지 않으려 해요.",
			"배려심이 있고 말과 행동이 크게 다르지 않은 사람이 좋아요."
		),
		new DummyCompletedUserSeed(
			4,
			"도미",
			true,
			1998L,
			AnimalProfile.DOG,
			Mbti.ENFP,
			ContactType.INSTAGRAM,
			"domi_004",
			"저는 처음에는 차분하지만 친해지면 장난도 많고 대화를 오래 이어가는 편이에요.",
			"서로의 일상을 편하게 공유하고 작은 약속도 잘 지키는 관계를 좋아해요.",
			"쉬는 날에는 산책하거나 맛있는 카페를 찾아다니는 시간을 좋아해요.",
			"천천히 알아가면서도 표현은 솔직하게 하는 편이에요.",
			"대화가 잘 통하고 서로를 배려할 줄 아는 사람이 좋아요."
		),
		new DummyCompletedUserSeed(
			5,
			"유찬",
			true,
			1996L,
			AnimalProfile.FOX,
			Mbti.INTJ,
			ContactType.KAKAO,
			"yuchan_005",
			"생각이 깊은 편이고 관심 있는 주제는 오래 이야기하는 걸 좋아해요.",
			"서로의 목표를 존중하면서도 필요할 때 든든한 편이 되고 싶어요.",
			"음악을 듣거나 조용한 곳에서 산책하며 생각을 정리하는 시간이 좋아요.",
			"처음에는 신중하지만 마음이 생기면 오래 진심을 지키는 편이에요.",
			"자기만의 취향이 있고 솔직하게 소통하는 사람이 좋아요."
		),
		new DummyCompletedUserSeed(
			6,
			"하준",
			true,
			1997L,
			AnimalProfile.BEAR,
			Mbti.ISFJ,
			ContactType.INSTAGRAM,
			"hajun_006",
			"책임감이 강하고 한번 가까워진 사람에게는 오래 마음을 쓰는 편이에요.",
			"편안한 안정감 속에서 서로를 응원하는 관계를 만들고 싶어요.",
			"운동을 하거나 집에서 영화를 보며 쉬는 시간을 좋아해요.",
			"연락과 약속을 중요하게 생각하고 꾸준히 챙기는 편이에요.",
			"성실하고 자신의 생활을 잘 가꾸는 사람이 매력적으로 느껴져요."
		)
	);

	private final UserRepository userRepository;
	private final CardRepository cardRepository;
	private final IntroductionRepository introductionRepository;
	private final ProfileImageRepository profileImageRepository;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		List<User> users = SEEDS.stream()
			.map(this::getOrCreateUser)
			.toList();

		assignTemporaryNicknames(users);

		for (int index = 0; index < SEEDS.size(); index++) {
			applyCompletedProfile(SEEDS.get(index), users.get(index), users);
		}
		applyRelations(users);
	}

	private User getOrCreateUser(DummyCompletedUserSeed seed) {
		long kakaoId = DUMMY_KAKAO_ID_START + seed.index() - 1;
		return userRepository.findByKakaoId(kakaoId)
			.orElseGet(() -> userRepository.save(User.create(kakaoId)));
	}

	private void applyCompletedProfile(DummyCompletedUserSeed seed, User user, List<User> users) {
		user.setName("더미 유저 " + seed.index());
		user.setNickname(seed.nickname());
		user.setGender(seed.gender());
		user.setBirthYear(seed.birthYear());
		user.setAnimalProfile(seed.animalProfile());
		user.setCookies(20L);
		user.setContactType(seed.contactType());
		user.setContact(seed.contact());
		user.setInviteCode("DUMMY" + displayNumber(seed.index()));
		user.setNotificationPhone("0100000" + displayNumber(seed.index()));
		user.setIsRegistered(true);
		user.setIsProfileCompleted(true);
		user.setHasIntroduction(true);
		user.setRefreshAt(LocalDateTime.now());
		user.setFreeBlurCount(0);
		user.setFreeBlurResetAt(LocalDateTime.now());
		user.setFreeLikeCount(0);
		user.setFreeLikeResetAt(LocalDateTime.now());

		Card card = cardRepository.findByUserId(user.getId())
			.orElseGet(() -> {
				Card created = Card.create(
					user,
					seed.mbti(),
					seed.datingStyle(),
					seed.idealType(),
					DummyImageKeys.of(seed.index()).origin()
				);
				user.setCard(created);
				return cardRepository.save(created);
			});
		card.update(seed.mbti(), seed.datingStyle(), seed.idealType(), DummyImageKeys.of(seed.index()).origin());

		ProfileImage profileImage = profileImageRepository.findByUserId(user.getId())
			.orElseGet(() -> {
				ProfileImage created = ProfileImage.create(user);
				return profileImageRepository.save(created);
			});
		DummyImageKeys keys = DummyImageKeys.of(seed.index());
		profileImage.prepareUpload(
			1L,
			keys.origin(),
			keys.originBlur(),
			keys.thumbnail(),
			keys.thumbnailBlur()
		);
		profileImage.markReady();

		if (user.getIntroduction() == null) {
			User introducer = users.get(seed.index() % users.size());
			Introduction introduction = Introduction.create(
				seed.q1(),
				seed.q2(),
				seed.q3(),
				introducer,
				"DUMMY" + displayNumber(seed.index())
			);
			introduction.assignParticipant(user);
			introductionRepository.save(introduction);
		} else {
			User introducer = users.get(seed.index() % users.size());
			user.getIntroduction().update(
				seed.q1(),
				seed.q2(),
				seed.q3(),
				introducer,
				"DUMMY" + displayNumber(seed.index())
			);
		}
	}

	private void assignTemporaryNicknames(List<User> users) {
		for (User user : users) {
			user.setNickname("TMP" + user.getKakaoId());
		}
		userRepository.flush();
	}

	private void applyRelations(List<User> users) {
		int size = users.size();
		for (int index = 0; index < size; index++) {
			User user = users.get(index);
			user.setNowShows(new ArrayList<>());
			user.setMyFans(new ArrayList<>());
			user.setMyTypes(new ArrayList<>());
			user.setMyBlurs(new ArrayList<>());

			for (int offset = 1; offset < size; offset++) {
				user.getNowShows().add(users.get((index + offset) % size).getId());
			}

			addLike(user, users.get((index + 1) % size));
			addLike(user, users.get((index + 2) % size));

			if (index % 2 == 0) {
				User mutualTarget = users.get((index + 1) % size);
				addLike(mutualTarget, user);
				addUnique(user.getMyBlurs(), mutualTarget.getId());
				addUnique(mutualTarget.getMyBlurs(), user.getId());
			}
		}
	}

	private void addLike(User requester, User target) {
		addUnique(requester.getMyTypes(), target.getId());
		addUnique(target.getMyFans(), requester.getId());
	}

	private void addUnique(List<Long> values, Long value) {
		if (!values.contains(value)) {
			values.add(value);
		}
	}

	private String displayNumber(int index) {
		return String.format("%03d", index);
	}

	private record DummyCompletedUserSeed(
		int index,
		String nickname,
		boolean gender,
		Long birthYear,
		AnimalProfile animalProfile,
		Mbti mbti,
		ContactType contactType,
		String contact,
		String q1,
		String q2,
		String q3,
		String datingStyle,
		String idealType
	) {
	}

	private record DummyImageKeys(
		String origin,
		String thumbnail,
		String thumbnailBlur,
		String originBlur
	) {

		private static DummyImageKeys of(int index) {
			String basePath = "dummy/profile-images/dummy" + index;
			return new DummyImageKeys(
				basePath + "/origin.jpg",
				basePath + "/thumbnail.jpg",
				basePath + "/thumbnail-blur.jpg",
				basePath + "/origin-blur.jpg"
			);
		}
	}
}
