package com.domisa.domisa_backend.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.domisa.domisa_backend.notification.service.NotificationService;
import com.domisa.domisa_backend.notification.type.NotificationType;
import com.domisa.domisa_backend.payment.config.PayActionProperties;
import com.domisa.domisa_backend.payment.dto.PayActionMatchCompleteWebhookRequest;
import com.domisa.domisa_backend.payment.entity.CookieOrder;
import com.domisa.domisa_backend.payment.entity.CookieTransaction;
import com.domisa.domisa_backend.payment.entity.PayActionWebhookLog;
import com.domisa.domisa_backend.payment.repository.CookieOrderRepository;
import com.domisa.domisa_backend.payment.repository.CookieTransactionRepository;
import com.domisa.domisa_backend.payment.repository.PayActionWebhookLogRepository;
import com.domisa.domisa_backend.global.exception.GlobalException;
import com.domisa.domisa_backend.user.entity.User;
import com.domisa.domisa_backend.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PayActionWebhookServiceTest {

	@Mock
	private CookieOrderRepository cookieOrderRepository;

	@Mock
	private CookieTransactionRepository cookieTransactionRepository;

	@Mock
	private PayActionWebhookLogRepository payActionWebhookLogRepository;

	@Mock
	private NotificationService notificationService;

	@Mock
	private UserRepository userRepository;

	private PayActionWebhookService payActionWebhookService;

	@BeforeEach
	void setUp() {
		PayActionProperties properties = new PayActionProperties();
		properties.setWebhookKey("webhook-key");
		properties.setMallId("mall-id");
		payActionWebhookService = new PayActionWebhookService(
			properties,
			cookieOrderRepository,
			cookieTransactionRepository,
			payActionWebhookLogRepository,
			notificationService,
			userRepository
		);
	}

	@Test
	void handleMatchCompleteRejectsWhenWebhookKeyDiffers() {
		assertThatThrownBy(() -> payActionWebhookService.handleMatchComplete(
			"wrong",
			"mall-id",
			"trace",
			new PayActionMatchCompleteWebhookRequest("CK20260502000001", "매칭완료", "2026-05-02T15:32:00+09:00")
		)).isInstanceOf(GlobalException.class);

		verify(cookieOrderRepository, never()).findByOrderNumberWithLock(any());
	}

	@Test
	void handleMatchCompleteRejectsWhenMallIdDiffers() {
		assertThatThrownBy(() -> payActionWebhookService.handleMatchComplete(
			"webhook-key",
			"wrong",
			"trace",
			new PayActionMatchCompleteWebhookRequest("CK20260502000001", "매칭완료", "2026-05-02T15:32:00+09:00")
		)).isInstanceOf(GlobalException.class);

		verify(cookieOrderRepository, never()).findByOrderNumberWithLock(any());
	}

	@Test
	void handleMatchCompleteReturnsWhenOrderStatusIsNotMatched() {
		payActionWebhookService.handleMatchComplete(
			"webhook-key",
			"mall-id",
			"trace",
			new PayActionMatchCompleteWebhookRequest("CK20260502000001", "대기중", "2026-05-02T15:32:00+09:00")
		);

		verify(cookieOrderRepository, never()).findByOrderNumberWithLock(any());
	}

	@Test
	void handleMatchCompleteMarksOrderPaidAndChargesUserCookies() {
		User user = createUser(1L, 5L);
		CookieOrder order = CookieOrder.create(
			user,
			"CK20260502000001",
			"A7B9",
			10000,
			100,
			"홍길동",
			LocalDateTime.of(2026, 5, 2, 15, 30)
		);
		when(payActionWebhookLogRepository.existsByTraceId("trace-1")).thenReturn(false);
		when(cookieOrderRepository.findByOrderNumberWithLock("CK20260502000001")).thenReturn(Optional.of(order));
		when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(user));

		payActionWebhookService.handleMatchComplete(
			"webhook-key",
			"mall-id",
			"trace-1",
			new PayActionMatchCompleteWebhookRequest("CK20260502000001", "매칭완료", "2026-05-02T15:32:00+09:00")
		);

		assertThat(order.isPaid()).isTrue();
		assertThat(order.getPayactionProcessingDate()).isEqualTo(LocalDateTime.of(2026, 5, 2, 15, 32));
		assertThat(user.getCookieBalance()).isEqualTo(105L);

		ArgumentCaptor<CookieTransaction> transactionCaptor = ArgumentCaptor.forClass(CookieTransaction.class);
		verify(cookieTransactionRepository).save(transactionCaptor.capture());
		assertThat(transactionCaptor.getValue().getAmount()).isEqualTo(100);
		verify(notificationService).createNotification(NotificationType.COOKIE_PAYMENT, 1L, null);

		ArgumentCaptor<PayActionWebhookLog> logCaptor = ArgumentCaptor.forClass(PayActionWebhookLog.class);
		verify(payActionWebhookLogRepository).saveAndFlush(logCaptor.capture());
		assertThat(logCaptor.getValue().getTraceId()).isEqualTo("trace-1");
	}

	@Test
	void handleMatchCompleteIsIdempotentWhenAlreadyPaid() {
		User user = createUser(1L, 5L);
		CookieOrder order = CookieOrder.create(
			user,
			"CK20260502000001",
			"A7B9",
			10000,
			100,
			"홍길동",
			LocalDateTime.of(2026, 5, 2, 15, 30)
		);
		order.markPaid(LocalDateTime.of(2026, 5, 2, 15, 32));
		when(payActionWebhookLogRepository.existsByTraceId("trace-1")).thenReturn(false);
		when(cookieOrderRepository.findByOrderNumberWithLock("CK20260502000001")).thenReturn(Optional.of(order));

		payActionWebhookService.handleMatchComplete(
			"webhook-key",
			"mall-id",
			"trace-1",
			new PayActionMatchCompleteWebhookRequest("CK20260502000001", "매칭완료", "2026-05-02T15:32:00+09:00")
		);

		verify(userRepository, never()).findByIdWithLock(any());
		verify(cookieTransactionRepository, never()).save(any());
		verify(notificationService, never()).createNotification(any(), any(), any());
		verify(payActionWebhookLogRepository).saveAndFlush(any());
	}

	@Test
	void handleMatchCompleteIgnoresDuplicatedTraceIdWhenLogSaveConflicts() {
		User user = createUser(1L, 5L);
		CookieOrder order = CookieOrder.create(
			user,
			"CK20260502000001",
			"A7B9",
			10000,
			100,
			"홍길동",
			LocalDateTime.of(2026, 5, 2, 15, 30)
		);
		order.markPaid(LocalDateTime.of(2026, 5, 2, 15, 32));
		when(payActionWebhookLogRepository.existsByTraceId("trace-1")).thenReturn(false);
		when(cookieOrderRepository.findByOrderNumberWithLock("CK20260502000001")).thenReturn(Optional.of(order));
		when(payActionWebhookLogRepository.saveAndFlush(any(PayActionWebhookLog.class)))
			.thenThrow(new DataIntegrityViolationException("duplicate trace"));

		payActionWebhookService.handleMatchComplete(
			"webhook-key",
			"mall-id",
			"trace-1",
			new PayActionMatchCompleteWebhookRequest("CK20260502000001", "매칭완료", "2026-05-02T15:32:00+09:00")
		);

		verify(userRepository, never()).findByIdWithLock(any());
		verify(cookieTransactionRepository, never()).save(any());
		verify(notificationService, never()).createNotification(any(), any(), any());
	}

	@Test
	void handleMatchCompleteOnlyChargesOnceWhenSameWebhookArrivesTwice() {
		User user = createUser(1L, 0L);
		CookieOrder order = CookieOrder.create(
			user,
			"CK20260502000001",
			"A7B9",
			10000,
			100,
			"홍길동",
			LocalDateTime.of(2026, 5, 2, 15, 30)
		);
		when(payActionWebhookLogRepository.existsByTraceId("trace-1")).thenReturn(false);
		when(payActionWebhookLogRepository.existsByTraceId("trace-2")).thenReturn(false);
		when(cookieOrderRepository.findByOrderNumberWithLock("CK20260502000001")).thenReturn(Optional.of(order));
		when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(user));

		PayActionMatchCompleteWebhookRequest request =
			new PayActionMatchCompleteWebhookRequest("CK20260502000001", "매칭완료", "2026-05-02T15:32:00+09:00");
		payActionWebhookService.handleMatchComplete("webhook-key", "mall-id", "trace-1", request);
		payActionWebhookService.handleMatchComplete("webhook-key", "mall-id", "trace-2", request);

		assertThat(user.getCookieBalance()).isEqualTo(100L);
		verify(cookieTransactionRepository, times(1)).save(any());
		verify(notificationService, times(1)).createNotification(NotificationType.COOKIE_PAYMENT, 1L, null);
	}

	@Test
	void handleMatchCompleteWorksWithoutTraceId() {
		User user = createUser(1L, 0L);
		CookieOrder order = CookieOrder.create(
			user,
			"CK20260502000001",
			"A7B9",
			10000,
			100,
			"홍길동",
			LocalDateTime.of(2026, 5, 2, 15, 30)
		);
		when(cookieOrderRepository.findByOrderNumberWithLock("CK20260502000001")).thenReturn(Optional.of(order));
		when(userRepository.findByIdWithLock(1L)).thenReturn(Optional.of(user));

		payActionWebhookService.handleMatchComplete(
			"webhook-key",
			"mall-id",
			null,
			new PayActionMatchCompleteWebhookRequest("CK20260502000001", "매칭완료", "2026-05-02T15:32:00+09:00")
		);

		assertThat(order.isPaid()).isTrue();
		assertThat(user.getCookieBalance()).isEqualTo(100L);
		verify(payActionWebhookLogRepository, never()).existsByTraceId(any());
		verify(payActionWebhookLogRepository, never()).saveAndFlush(any());
	}

	@Test
	void handleMatchCompleteReturnsWhenTraceIdAlreadyExists() {
		when(payActionWebhookLogRepository.existsByTraceId("trace-1")).thenReturn(true);

		payActionWebhookService.handleMatchComplete(
			"webhook-key",
			"mall-id",
			"trace-1",
			new PayActionMatchCompleteWebhookRequest("CK20260502000001", "매칭완료", "2026-05-02T15:32:00+09:00")
		);

		verify(cookieOrderRepository, never()).findByOrderNumberWithLock(any());
		verify(cookieTransactionRepository, never()).save(any());
		verify(notificationService, never()).createNotification(any(), any(), any());
	}

	private User createUser(Long id, Long cookies) {
		User user = User.create(1234L);
		ReflectionTestUtils.setField(user, "id", id);
		user.setPublicId("U0001");
		user.setCookies(cookies);
		return user;
	}
}
