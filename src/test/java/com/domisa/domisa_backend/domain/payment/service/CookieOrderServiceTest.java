package com.domisa.domisa_backend.domain.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.domisa.domisa_backend.domain.payment.client.PayActionClient;
import com.domisa.domisa_backend.domain.payment.config.PayActionProperties;
import com.domisa.domisa_backend.domain.payment.dto.CreateCookieOrderRequest;
import com.domisa.domisa_backend.domain.payment.dto.CreateCookieOrderResponse;
import com.domisa.domisa_backend.domain.payment.dto.PayActionCreateOrderRequest;
import com.domisa.domisa_backend.domain.payment.dto.PayActionCreateOrderResponse;
import com.domisa.domisa_backend.domain.payment.dto.PayActionResponseBody;
import com.domisa.domisa_backend.domain.payment.entity.CookieOrder;
import com.domisa.domisa_backend.domain.payment.entity.OrderStatus;
import com.domisa.domisa_backend.domain.payment.repository.CookieOrderRepository;
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
		PayActionProperties properties = new PayActionProperties();
		PayActionProperties.Deposit deposit = new PayActionProperties.Deposit();
		deposit.setBankName("국민은행");
		deposit.setBankCode("004");
		deposit.setAccountNumber("12345678901234");
		deposit.setAccountHolder("주식회사 도미사");
		properties.setDeposit(deposit);
		cookieOrderService = org.mockito.Mockito.spy(
			new CookieOrderService(userRepository, cookieOrderRepository, payActionClient, properties)
		);
	}

	@Test
	void createCookieOrderCreatesPendingOrderAndReturnsDepositGuide() {
		User user = createUser(1L, "홍길동", "길동", "U0001");
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(cookieOrderRepository.findTopByOrderNumberStartingWithOrderByOrderNumberDesc(any())).thenReturn(Optional.empty());
		when(cookieOrderRepository.existsByBillingNameAndOrderAmountAndStatus(any(), any(), any())).thenReturn(false);
		when(cookieOrderRepository.saveAndFlush(any(CookieOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(payActionClient.createOrder(any())).thenReturn(new PayActionCreateOrderResponse("success", new PayActionResponseBody(null)));
		doReturn(LocalDateTime.of(2026, 5, 2, 15, 30, 0)).when(cookieOrderService).currentKoreaDateTime();
		doReturn("A7B9").when(cookieOrderService).nextBillingNameCandidate();

		CreateCookieOrderResponse response = cookieOrderService.createCookieOrder(
			1L,
			new CreateCookieOrderRequest(100, 10000)
		);

		ArgumentCaptor<CookieOrder> orderCaptor = ArgumentCaptor.forClass(CookieOrder.class);
		verify(cookieOrderRepository).saveAndFlush(orderCaptor.capture());
		CookieOrder savedOrder = orderCaptor.getValue();
		assertThat(savedOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
		assertThat(savedOrder.getCookieAmount()).isEqualTo(100);
		assertThat(savedOrder.getOrderAmount()).isEqualTo(10000);
		assertThat(savedOrder.getBillingName()).isEqualTo("A7B9");
		assertThat(savedOrder.getOrderNumber()).isEqualTo("CK20260502000001");

		ArgumentCaptor<PayActionCreateOrderRequest> payActionCaptor = ArgumentCaptor.forClass(PayActionCreateOrderRequest.class);
		verify(payActionClient).createOrder(payActionCaptor.capture());
		PayActionCreateOrderRequest payActionRequest = payActionCaptor.getValue();
		assertThat(payActionRequest.orderNumber()).isEqualTo("CK20260502000001");
		assertThat(payActionRequest.orderAmount()).isEqualTo(10000);
		assertThat(payActionRequest.orderDate()).isEqualTo("2026-05-02T15:30+09:00");
		assertThat(payActionRequest.billingName()).isEqualTo("A7B9");
		assertThat(payActionRequest.ordererName()).isEqualTo("홍길동");

		assertThat(response.orderNumber()).isEqualTo("CK20260502000001");
		assertThat(response.orderAmount()).isEqualTo(10000);
		assertThat(response.billingName()).isEqualTo("A7B9");
		assertThat(response.bankName()).isEqualTo("국민은행");
		assertThat(response.bankCode()).isEqualTo("004");
		assertThat(response.accountNumber()).isEqualTo("12345678901234");
		assertThat(response.accountHolder()).isEqualTo("주식회사 도미사");
		assertThat(response.status()).isEqualTo("PENDING");
	}

	@Test
	void createCookieOrderThrowsWhenPayActionReturnsError() {
		User user = createUser(1L, "홍길동", "길동", "U0001");
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(cookieOrderRepository.findTopByOrderNumberStartingWithOrderByOrderNumberDesc(any())).thenReturn(Optional.empty());
		when(cookieOrderRepository.existsByBillingNameAndOrderAmountAndStatus(any(), any(), any())).thenReturn(false);
		when(cookieOrderRepository.saveAndFlush(any(CookieOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(payActionClient.createOrder(any())).thenReturn(
			new PayActionCreateOrderResponse("error", new PayActionResponseBody("이미 해당 주문번호의 주문이 존재합니다."))
		);
		doReturn(LocalDateTime.of(2026, 5, 2, 15, 30, 0)).when(cookieOrderService).currentKoreaDateTime();
		doReturn("A7B9").when(cookieOrderService).nextBillingNameCandidate();

		assertThatThrownBy(() -> cookieOrderService.createCookieOrder(1L, new CreateCookieOrderRequest(100, 10000)))
			.isInstanceOf(GlobalException.class)
			.hasMessage("페이액션 주문 등록 실패: 이미 해당 주문번호의 주문이 존재합니다.");
	}

	@Test
	void createCookieOrderRegeneratesBillingNameWhenPendingCombinationExists() {
		User user = createUser(1L, "홍길동", "길동", "U0001");
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(cookieOrderRepository.findTopByOrderNumberStartingWithOrderByOrderNumberDesc(any())).thenReturn(Optional.empty());
		when(cookieOrderRepository.existsByBillingNameAndOrderAmountAndStatus("A7B9", 10000, OrderStatus.PENDING)).thenReturn(true);
		when(cookieOrderRepository.existsByBillingNameAndOrderAmountAndStatus("B8C1", 10000, OrderStatus.PENDING)).thenReturn(false);
		when(cookieOrderRepository.saveAndFlush(any(CookieOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(payActionClient.createOrder(any())).thenReturn(new PayActionCreateOrderResponse("success", new PayActionResponseBody(null)));
		doReturn(LocalDateTime.of(2026, 5, 2, 15, 30, 0)).when(cookieOrderService).currentKoreaDateTime();
		doReturn("A7B9", "B8C1").when(cookieOrderService).nextBillingNameCandidate();

		CreateCookieOrderResponse response = cookieOrderService.createCookieOrder(
			1L,
			new CreateCookieOrderRequest(100, 10000)
		);

		assertThat(response.billingName()).isEqualTo("B8C1");
		verify(cookieOrderRepository, times(2))
			.existsByBillingNameAndOrderAmountAndStatus(any(), any(), any());
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
