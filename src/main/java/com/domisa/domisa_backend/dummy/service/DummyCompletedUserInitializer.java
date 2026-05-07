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
		),
		new DummyCompletedUserSeed(
			7,
			"민재",
			true,
			1999L,
			AnimalProfile.WOLF,
			Mbti.ISTP,
			ContactType.KAKAO,
			"minjae_007",
			"새로운 일을 직접 해보면서 배우는 걸 좋아하고 호기심이 많은 편이에요.",
			"서로에게 부담을 주기보다 편하게 응원하는 연애를 하고 싶어요.",
			"사진 찍으러 걷거나 늦은 밤 조용한 카페에 가는 걸 좋아해요.",
			"말을 많이 하기보다는 필요한 순간에 확실히 표현하려고 해요.",
			"자연스럽게 대화가 이어지고 자기 시간을 잘 즐기는 사람이 좋아요."
		),
		new DummyCompletedUserSeed(
			8,
			"지호",
			true,
			2000L,
			AnimalProfile.OTTER,
			Mbti.ESFJ,
			ContactType.INSTAGRAM,
			"jiho_008",
			"사람들과 어울리는 걸 좋아하고 주변 분위기를 잘 살피는 편이에요.",
			"서로의 하루를 자주 나누고 작은 기념일도 챙기는 연애가 좋아요.",
			"친구들과 맛집을 찾거나 새로운 전시를 보는 시간을 즐겨요.",
			"상대가 편안하게 느끼도록 먼저 다가가고 챙기는 편이에요.",
			"밝고 고마움을 잘 표현하는 사람이 오래 기억에 남아요."
		),
		new DummyCompletedUserSeed(
			9,
			"태윤",
			true,
			1997L,
			AnimalProfile.ALPACA,
			Mbti.ENTJ,
			ContactType.KAKAO,
			"taeyun_009",
			"계획을 세워 움직이는 걸 좋아하고 맡은 일은 끝까지 해내려고 해요.",
			"서로의 목표를 응원하면서 함께 성장하는 관계를 만들고 싶어요.",
			"운동을 하거나 새로운 장소를 찾아가며 에너지를 채우는 편이에요.",
			"좋아하는 사람에게는 솔직하고 책임감 있게 행동하려고 해요.",
			"자기 생각이 분명하고 약속을 소중히 여기는 사람이 좋아요."
		),
		new DummyCompletedUserSeed(
			10,
			"준서",
			true,
			2001L,
			AnimalProfile.HAMSTER,
			Mbti.INTP,
			ContactType.INSTAGRAM,
			"junseo_010",
			"혼자 집중하는 시간도 좋아하지만 친해지면 엉뚱한 이야기를 많이 해요.",
			"서로의 취향을 천천히 알아가며 편하게 웃는 관계를 선호해요.",
			"영화나 게임 이야기를 나누고 새로운 음악을 찾아듣는 걸 좋아해요.",
			"감정 표현은 서툴 수 있지만 진심은 꾸준히 보여주려고 해요.",
			"관심사를 존중해주고 차분하게 대화할 수 있는 사람이 좋아요."
		),
		new DummyCompletedUserSeed(
			11,
			"시우",
			true,
			1998L,
			AnimalProfile.CAPYBARA,
			Mbti.ESTP,
			ContactType.KAKAO,
			"siwoo_011",
			"활동적인 편이라 새로운 장소와 새로운 사람을 만나는 걸 즐겨요.",
			"같이 재미있는 경험을 쌓고 솔직하게 표현하는 연애가 좋아요.",
			"날씨가 좋으면 드라이브를 가거나 운동으로 시간을 보내요.",
			"마음이 생기면 먼저 연락하고 만남을 자연스럽게 제안하는 편이에요.",
			"웃음이 많고 순간을 같이 즐길 줄 아는 사람이 매력적이에요."
		),
		new DummyCompletedUserSeed(
			12,
			"현우",
			true,
			1995L,
			AnimalProfile.DOG,
			Mbti.ESTJ,
			ContactType.INSTAGRAM,
			"hyunwoo_012",
			"생활 패턴이 일정하고 해야 할 일을 미루지 않으려는 편이에요.",
			"신뢰를 쌓아가며 안정적으로 오래 만나는 관계를 원해요.",
			"주말에는 운동을 하거나 좋은 식당을 예약해 다녀오는 걸 좋아해요.",
			"상대가 불안하지 않도록 약속과 연락을 분명히 하는 편이에요.",
			"성실하고 자신의 삶을 단단하게 꾸려가는 사람이 좋아요."
		),
		new DummyCompletedUserSeed(
			13,
			"도윤",
			true,
			1999L,
			AnimalProfile.FOX,
			Mbti.ISFP,
			ContactType.KAKAO,
			"doyoon_013",
			"조용한 공간에서 편하게 이야기하는 걸 좋아하고 감성이 섬세한 편이에요.",
			"억지로 맞추기보다 서로의 속도를 존중하는 연애를 하고 싶어요.",
			"사진을 보정하거나 산책하면서 새로운 풍경을 찾는 시간이 좋아요.",
			"좋아하는 마음은 작은 선물이나 행동으로 자연스럽게 표현해요.",
			"따뜻한 말투와 편안한 분위기를 가진 사람이 좋아요."
		),
		new DummyCompletedUserSeed(
			14,
			"유나",
			false,
			2002L,
			AnimalProfile.CAT,
			Mbti.ESFP,
			ContactType.INSTAGRAM,
			"yuna_014",
			"즉흥적인 만남과 재미있는 대화를 좋아해서 주변을 밝게 만드는 편이에요.",
			"함께 있을 때 웃음이 많고 감정을 솔직히 나누는 연애가 좋아요.",
			"예쁜 카페를 찾거나 노래를 들으며 걷는 시간을 즐겨요.",
			"호감이 생기면 숨기기보다 자연스럽게 표현하는 편이에요.",
			"긍정적이고 사소한 순간도 즐길 줄 아는 사람이 좋아요."
		),
		new DummyCompletedUserSeed(
			15,
			"채린",
			false,
			1998L,
			AnimalProfile.RABBIT,
			Mbti.INFJ,
			ContactType.KAKAO,
			"chaerin_015",
			"상대의 마음을 오래 생각하고 깊은 대화를 나누는 걸 좋아해요.",
			"서로를 조용히 응원하면서도 중요한 순간에는 곁에 있는 관계를 원해요.",
			"책을 읽거나 잔잔한 음악이 나오는 공간에서 쉬는 걸 좋아해요.",
			"쉽게 다가가지는 않지만 마음이 생기면 진심을 오래 지켜요.",
			"다정하고 말의 무게를 아는 사람이 좋게 느껴져요."
		),
		new DummyCompletedUserSeed(
			16,
			"소율",
			false,
			2000L,
			AnimalProfile.DEER,
			Mbti.ENTP,
			ContactType.INSTAGRAM,
			"soyul_016",
			"새로운 아이디어를 이야기하는 걸 좋아하고 대화가 빠르게 이어지는 편이에요.",
			"서로에게 좋은 자극을 주면서도 가볍게 웃을 수 있는 연애가 좋아요.",
			"보드게임이나 전시처럼 같이 이야기할 거리가 많은 활동을 즐겨요.",
			"호기심이 생기면 먼저 질문하고 자연스럽게 가까워지는 편이에요.",
			"재치 있고 자신의 생각을 즐겁게 나눌 수 있는 사람이 좋아요."
		),
		new DummyCompletedUserSeed(
			17,
			"하린",
			false,
			1997L,
			AnimalProfile.OTTER,
			Mbti.ISFP,
			ContactType.KAKAO,
			"harin_017",
			"편안하고 부드러운 분위기를 좋아하고 작은 것에 감동을 잘 받아요.",
			"서로를 재촉하지 않고 천천히 익숙해지는 관계를 만들고 싶어요.",
			"맛있는 디저트를 먹거나 강가를 산책하는 시간을 좋아해요.",
			"표현은 조용하지만 상대를 위해 세심하게 챙기려고 해요.",
			"차분하고 배려가 자연스러운 사람이 오래 보고 싶어져요."
		),
		new DummyCompletedUserSeed(
			18,
			"지안",
			false,
			2001L,
			AnimalProfile.HAMSTER,
			Mbti.ESTJ,
			ContactType.INSTAGRAM,
			"jian_018",
			"깔끔한 계획과 분명한 소통을 좋아하고 맡은 일은 책임감 있게 해요.",
			"서로 믿을 수 있고 일상을 안정적으로 공유하는 연애가 좋아요.",
			"일정을 정해 맛집을 가거나 새로운 취미를 배워보는 걸 즐겨요.",
			"마음이 생기면 애매하게 굴기보다 확실히 표현하려고 해요.",
			"성실하고 말과 행동이 일치하는 사람이 좋아요."
		),
		new DummyCompletedUserSeed(
			19,
			"다은",
			false,
			1999L,
			AnimalProfile.ALPACA,
			Mbti.INTP,
			ContactType.KAKAO,
			"daeun_019",
			"관심 있는 분야를 깊게 파고드는 걸 좋아하고 관찰을 많이 하는 편이에요.",
			"서로의 생각을 존중하면서 편하게 질문할 수 있는 관계를 원해요.",
			"조용한 카페에서 글을 쓰거나 새로운 콘텐츠를 보는 시간을 좋아해요.",
			"감정 표현은 천천히 하지만 신뢰가 쌓이면 솔직해지는 편이에요.",
			"차분하게 대화하고 서로의 세계를 궁금해하는 사람이 좋아요."
		),
		new DummyCompletedUserSeed(
			20,
			"예린",
			false,
			2002L,
			AnimalProfile.CAPYBARA,
			Mbti.ENFP,
			ContactType.INSTAGRAM,
			"yerin_020",
			"밝고 호기심이 많아서 새로운 사람의 이야기를 듣는 걸 좋아해요.",
			"서로에게 웃을 일이 많고 힘든 날에는 가장 먼저 떠오르는 관계가 좋아요.",
			"공연을 보거나 계절마다 다른 장소를 찾아다니는 걸 즐겨요.",
			"좋아하는 마음이 생기면 말과 행동으로 자주 보여주려 해요.",
			"따뜻하고 유머가 있으며 같이 있으면 편한 사람이 좋아요."
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
		user.setRefreshAvailableAt(LocalDateTime.now().withNano(0).plusHours(2));
		user.setFreeLikeCount(3);

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
		for (User user : users) {
			user.setNowShows(new ArrayList<>());
			user.setBeforeShows(new ArrayList<>());
			user.setMyFans(new ArrayList<>());
			user.setMyTypes(new ArrayList<>());
			user.setMyMatches(new ArrayList<>());
			user.setMyBlurs(new ArrayList<>());
		}

		for (int index = 0; index < size; index++) {
			User user = users.get(index);

			for (int offset = 1; offset < size; offset++) {
				User target = users.get((index + offset) % size);
				if (user.getGender() != null && target.getGender() != null && !user.getGender().equals(target.getGender())) {
					user.getNowShows().add(target.getId());
				}
			}

			addLike(user, users.get((index + 1) % size));
			addLike(user, users.get((index + 2) % size));

			if (index % 2 == 0) {
				User mutualTarget = users.get((index + 1) % size);
				addLike(mutualTarget, user);
				addMatch(user, mutualTarget);
				addUnique(user.getMyBlurs(), mutualTarget.getId());
				addUnique(mutualTarget.getMyBlurs(), user.getId());
			}
		}
	}

	private void addLike(User requester, User target) {
		ensureRelationLists(requester);
		ensureRelationLists(target);
		addUnique(requester.getMyTypes(), target.getId());
		addUnique(target.getMyFans(), requester.getId());
	}

	private void addMatch(User user, User target) {
		ensureRelationLists(user);
		ensureRelationLists(target);
		addUnique(user.getMyMatches(), target.getId());
		addUnique(target.getMyMatches(), user.getId());
		user.getMyFans().remove(target.getId());
		user.getMyTypes().remove(target.getId());
		target.getMyFans().remove(user.getId());
		target.getMyTypes().remove(user.getId());
	}

	private void addUnique(List<Long> values, Long value) {
		if (value != null && !values.contains(value)) {
			values.add(value);
		}
	}

	private void ensureRelationLists(User user) {
		if (user.getNowShows() == null) {
			user.setNowShows(new ArrayList<>());
		}
		if (user.getMyFans() == null) {
			user.setMyFans(new ArrayList<>());
		}
		if (user.getMyTypes() == null) {
			user.setMyTypes(new ArrayList<>());
		}
		if (user.getMyMatches() == null) {
			user.setMyMatches(new ArrayList<>());
		}
		if (user.getMyBlurs() == null) {
			user.setMyBlurs(new ArrayList<>());
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
