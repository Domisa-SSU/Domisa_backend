package com.domisa.domisa_backend.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.domisa.domisa_backend.payment.client.PayActionClient;
import com.domisa.domisa_backend.payment.dto.CancelCookieOrderRequest;
import com.domisa.domisa_backend.payment.dto.CreateCookieOrderRequest;
import com.domisa.domisa_backend.payment.dto.CreateCookieOrderResponse;
import com.domisa.domisa_backend.payment.dto.PayActionCreateOrderRequest;
import com.domisa.domisa_backend.payment.dto.PayActionCreateOrderResponse;
import com.domisa.domisa_backend.payment.dto.PayActionOrderExcludeRequest;
import com.domisa.domisa_backend.payment.dto.PayActionResponseBody;
import com.domisa.domisa_backend.payment.entity.CookieOrder;
import com.domisa.domisa_backend.payment.entity.OrderStatus;
import com.domisa.domisa_backend.payment.repository.CookieOrderRepository;
import com.domisa.domisa_backend.global.exception.GlobalException;
import com.domisa.domisa_backend.user.entity.User;
import com.domisa.domisa_backend.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CookieOrderServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private CookieOrderRepository cookieOrderRepository;

	@Mock
	private PayActionClient payActionClient;

	private CookieOrderService cookieOrderService;

	@BeforeEach
	void setUp() {
		cookieOrderService = org.mockito.Mockito.spy(
			new CookieOrderService(
				userRepository,
				cookieOrderRepository,
				payActionClient,
				new BillingNameGenerator(),
				new OrderNumberGenerator()
			)
		);
	}

	@Test
	void createCookieOrderCreatesPendingOrderAndReturnsBillingNameAndAmount() {
		User user = createUser(1L, "홍길동", "길동", "U0001");
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(cookieOrderRepository.findTopByOrderNumberStartingWithOrderByOrderNumberDesc(any())).thenReturn(Optional.empty());
		when(cookieOrderRepository.existsByBillingNameAndStatus(any(), any())).thenReturn(false);
		when(cookieOrderRepository.saveAndFlush(any(CookieOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(payActionClient.createOrder(any())).thenReturn(new PayActionCreateOrderResponse("success", new PayActionResponseBody(null)));
		doReturn(LocalDateTime.of(2026, 5, 2, 15, 30, 0)).when(cookieOrderService).currentKoreaDateTime();
		doReturn("입금A7K3Q9").when(cookieOrderService).nextBillingNameCandidate();

		CreateCookieOrderResponse response = cookieOrderService.createCookieOrder(
			1L,
			new CreateCookieOrderRequest(100, 10000)
		);

		ArgumentCaptor<CookieOrder> orderCaptor = ArgumentCaptor.forClass(CookieOrder.class);
		verify(cookieOrderRepository).saveAndFlush(orderCaptor.capture());
		CookieOrder savedOrder = orderCaptor.getValue();
		assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
		assertThat(savedOrder.getCookieAmount()).isEqualTo(100);
		assertThat(savedOrder.getOrderAmount()).isEqualTo(10000);
		assertThat(savedOrder.getBillingName()).isEqualTo("입금A7K3Q9");
		assertThat(savedOrder.getOrderNumber()).isEqualTo("ORD260502000001");
		assertThat(savedOrder.getOrdererName()).isEqualTo("홍길동");

		ArgumentCaptor<PayActionCreateOrderRequest> payActionCaptor = ArgumentCaptor.forClass(PayActionCreateOrderRequest.class);
		verify(payActionClient).createOrder(payActionCaptor.capture());
		PayActionCreateOrderRequest payActionRequest = payActionCaptor.getValue();
		assertThat(payActionRequest.orderNumber()).isEqualTo("ORD260502000001");
		assertThat(payActionRequest.orderAmount()).isEqualTo(10000);
		assertThat(payActionRequest.orderDate()).isEqualTo("2026-05-02T15:30:00+09:00");
		assertThat(payActionRequest.billingName()).isEqualTo("입금A7K3Q9");
		assertThat(payActionRequest.ordererName()).isEqualTo("홍길동");

		assertThat(response.billingName()).isEqualTo("입금A7K3Q9");
		assertThat(response.orderAmount()).isEqualTo(10000);
	}

	@Test
	void createCookieOrderThrowsWhenPayActionReturnsError() {
		User user = createUser(1L, "홍길동", "길동", "U0001");
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(cookieOrderRepository.findTopByOrderNumberStartingWithOrderByOrderNumberDesc(any())).thenReturn(Optional.empty());
		when(cookieOrderRepository.existsByBillingNameAndStatus(any(), any())).thenReturn(false);
		when(cookieOrderRepository.saveAndFlush(any(CookieOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(payActionClient.createOrder(any())).thenReturn(
			new PayActionCreateOrderResponse("error", new PayActionResponseBody("이미 해당 주문번호의 주문이 존재합니다."))
		);
		doReturn(LocalDateTime.of(2026, 5, 2, 15, 30, 0)).when(cookieOrderService).currentKoreaDateTime();
		doReturn("입금A7K3Q9").when(cookieOrderService).nextBillingNameCandidate();

		assertThatThrownBy(() -> cookieOrderService.createCookieOrder(
			1L,
			new CreateCookieOrderRequest(100, 10000)
		))
			.isInstanceOf(GlobalException.class)
			.hasMessage("페이액션 주문 등록 실패: 이미 해당 주문번호의 주문이 존재합니다.");
	}

	@Test
	void createCookieOrderRegeneratesBillingNameWhenPendingCombinationExists() {
		User user = createUser(1L, "홍길동", "길동", "U0001");
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(cookieOrderRepository.findTopByOrderNumberStartingWithOrderByOrderNumberDesc(any())).thenReturn(Optional.empty());
		when(cookieOrderRepository.existsByBillingNameAndStatus("입금A7K3Q9", OrderStatus.PAYMENT_PENDING)).thenReturn(true);
		when(cookieOrderRepository.existsByBillingNameAndStatus("입금B8C1D2", OrderStatus.PAYMENT_PENDING)).thenReturn(false);
		when(cookieOrderRepository.existsByBillingNameAndStatus("입금B8C1D2", OrderStatus.PENDING)).thenReturn(false);
		when(cookieOrderRepository.saveAndFlush(any(CookieOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(payActionClient.createOrder(any())).thenReturn(new PayActionCreateOrderResponse("success", new PayActionResponseBody(null)));
		doReturn(LocalDateTime.of(2026, 5, 2, 15, 30, 0)).when(cookieOrderService).currentKoreaDateTime();
		doReturn("입금A7K3Q9", "입금B8C1D2").when(cookieOrderService).nextBillingNameCandidate();

		CreateCookieOrderResponse response = cookieOrderService.createCookieOrder(
			1L,
			new CreateCookieOrderRequest(100, 10000)
		);

		assertThat(response.billingName()).isEqualTo("입금B8C1D2");
		verify(cookieOrderRepository, times(3))
			.existsByBillingNameAndStatus(any(), any());
	}

	@Test
	void cancelCookieOrderExcludesPayActionOrderAndMarksCanceled() {
		User user = createUser(1L, "홍길동", "길동", "U0001");
		CookieOrder order = CookieOrder.create(
			user,
			"ORD260502000001",
			"입금A7K3Q9",
			10000,
			100,
			"홍길동",
			LocalDateTime.of(2026, 5, 2, 15, 30)
		);
		when(cookieOrderRepository.findByUserIdAndBillingNameAndOrderAmountWithLock(
			1L,
			"입금A7K3Q9",
			10000
		)).thenReturn(Optional.of(order));
		when(payActionClient.excludeOrder(any())).thenReturn(
			new PayActionCreateOrderResponse("success", new PayActionResponseBody(null))
		);

		cookieOrderService.cancelCookieOrder(
			1L,
			new CancelCookieOrderRequest("입금A7K3Q9", 10000)
		);

		ArgumentCaptor<PayActionOrderExcludeRequest> payActionCaptor =
			ArgumentCaptor.forClass(PayActionOrderExcludeRequest.class);
		verify(payActionClient).excludeOrder(payActionCaptor.capture());
		assertThat(payActionCaptor.getValue().orderNumber()).isEqualTo("ORD260502000001");
		assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELED);
	}

	@Test
	void cancelCookieOrderThrowsWhenPayActionExcludeReturnsError() {
		User user = createUser(1L, "홍길동", "길동", "U0001");
		CookieOrder order = CookieOrder.create(
			user,
			"ORD260502000001",
			"입금A7K3Q9",
			10000,
			100,
			"홍길동",
			LocalDateTime.of(2026, 5, 2, 15, 30)
		);
		when(cookieOrderRepository.findByUserIdAndBillingNameAndOrderAmountWithLock(
			1L,
			"입금A7K3Q9",
			10000
		)).thenReturn(Optional.of(order));
		when(payActionClient.excludeOrder(any())).thenReturn(
			new PayActionCreateOrderResponse("error", new PayActionResponseBody("누락된 필드가 존재합니다."))
		);

		assertThatThrownBy(() -> cookieOrderService.cancelCookieOrder(
			1L,
			new CancelCookieOrderRequest("입금A7K3Q9", 10000)
		))
			.isInstanceOf(GlobalException.class)
			.hasMessage("페이액션 주문 매칭 제외 실패: 누락된 필드가 존재합니다.");
		assertThat(order.getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
	}

	@Test
	void getCookieOrderPaymentStatusReturnsCookieAmountWhenPaid() {
		User user = createUser(1L, "홍길동", "길동", "U0001");
		CookieOrder order = CookieOrder.create(
			user,
			"ORD260502000001",
			"입금A7K3Q9",
			10000,
			100,
			"홍길동",
			LocalDateTime.of(2026, 5, 2, 15, 30)
		);
		order.markPaid(LocalDateTime.of(2026, 5, 2, 15, 32));
		when(cookieOrderRepository.findByUserIdAndBillingNameAndOrderAmount(
			1L,
			"입금A7K3Q9",
			10000
		)).thenReturn(Optional.of(order));

		var response = cookieOrderService.getCookieOrderPaymentStatus(
			1L,
			"입금A7K3Q9",
			10000
		);

		assertThat(response.confirmed()).isTrue();
		assertThat(response.status()).isEqualTo("PAID");
		assertThat(response.cookieAmount()).isEqualTo(100);
	}

	@Test
	void getCookieOrderPaymentStatusReturnsUnconfirmedWhenOrderIsNotPaid() {
		User user = createUser(1L, "홍길동", "길동", "U0001");
		CookieOrder order = CookieOrder.create(
			user,
			"ORD260502000001",
			"입금A7K3Q9",
			10000,
			100,
			"홍길동",
			LocalDateTime.of(2026, 5, 2, 15, 30)
		);
		when(cookieOrderRepository.findByUserIdAndBillingNameAndOrderAmount(
			1L,
			"입금A7K3Q9",
			10000
		)).thenReturn(Optional.of(order));

		var response = cookieOrderService.getCookieOrderPaymentStatus(
			1L,
			"입금A7K3Q9",
			10000
		);

		assertThat(response.confirmed()).isFalse();
		assertThat(response.status()).isEqualTo("PAYMENT_PENDING");
		assertThat(response.cookieAmount()).isNull();
	}

	@Test
	void getCookieOrderPaymentStatusReturnsUnconfirmedWhenOrderIsNotFound() {
		when(cookieOrderRepository.findByUserIdAndBillingNameAndOrderAmount(
			1L,
			"입금A7K3Q9",
			10000
		)).thenReturn(Optional.empty());

		var response = cookieOrderService.getCookieOrderPaymentStatus(
			1L,
			"입금A7K3Q9",
			10000
		);

		assertThat(response.confirmed()).isFalse();
		assertThat(response.status()).isEqualTo("UNCONFIRMED");
		assertThat(response.cookieAmount()).isNull();
	}

	private User createUser(Long id, String name, String nickname, String publicId) {
		User user = User.create(1234L);
		ReflectionTestUtils.setField(user, "id", id);
		user.setName(name);
		user.setNickname(nickname);
		user.setPublicId(publicId);
		return user;
	}
}
