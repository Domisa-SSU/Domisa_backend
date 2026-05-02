package com.domisa.domisa_backend.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.domisa.domisa_backend.cookie.entity.CookieWallet;
import com.domisa.domisa_backend.cookie.repository.CookieWalletRepository;
import com.domisa.domisa_backend.payment.config.PayActionProperties;
import com.domisa.domisa_backend.payment.dto.PayActionMatchedWebhookRequest;
import com.domisa.domisa_backend.payment.entity.CookieOrder;
import com.domisa.domisa_backend.payment.entity.CookieTransaction;
import com.domisa.domisa_backend.payment.repository.CookieOrderRepository;
import com.domisa.domisa_backend.payment.repository.CookieTransactionRepository;
import com.domisa.domisa_backend.global.exception.GlobalException;
import com.domisa.domisa_backend.user.entity.User;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PayActionWebhookServiceTest {

	@Mock
	private CookieOrderRepository cookieOrderRepository;

	@Mock
	private CookieWalletRepository cookieWalletRepository;

	@Mock
	private CookieTransactionRepository cookieTransactionRepository;

	private PayActionWebhookService payActionWebhookService;

	@BeforeEach
	void setUp() {
		PayActionProperties properties = new PayActionProperties();
		properties.setWebhookKey("webhook-key");
		properties.setMallId("mall-id");
		payActionWebhookService = new PayActionWebhookService(
			properties,
			cookieOrderRepository,
			cookieWalletRepository,
			cookieTransactionRepository
		);
	}

	@Test
	void handleMatchedWebhookRejectsWhenWebhookKeyDiffers() {
		assertThatThrownBy(() -> payActionWebhookService.handleMatchedWebhook(
			"wrong",
			"mall-id",
			"trace",
			new PayActionMatchedWebhookRequest("CK20260502000001", "매칭완료", "2026-05-02T15:32:00+09:00")
		)).isInstanceOf(GlobalException.class);

		verify(cookieOrderRepository, never()).findByOrderNumberWithLock(any());
	}

	@Test
	void handleMatchedWebhookRejectsWhenMallIdDiffers() {
		assertThatThrownBy(() -> payActionWebhookService.handleMatchedWebhook(
			"webhook-key",
			"wrong",
			"trace",
			new PayActionMatchedWebhookRequest("CK20260502000001", "매칭완료", "2026-05-02T15:32:00+09:00")
		)).isInstanceOf(GlobalException.class);

		verify(cookieOrderRepository, never()).findByOrderNumberWithLock(any());
	}

	@Test
	void handleMatchedWebhookRejectsWhenOrderStatusIsNotMatched() {
		assertThatThrownBy(() -> payActionWebhookService.handleMatchedWebhook(
			"webhook-key",
			"mall-id",
			"trace",
			new PayActionMatchedWebhookRequest("CK20260502000001", "대기중", "2026-05-02T15:32:00+09:00")
		)).isInstanceOf(GlobalException.class);

		verify(cookieOrderRepository, never()).findByOrderNumberWithLock(any());
	}

	@Test
	void handleMatchedWebhookMarksOrderPaidAndChargesWallet() {
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
		CookieWallet wallet = CookieWallet.create(user, 5);
		when(cookieOrderRepository.findByOrderNumberWithLock("CK20260502000001")).thenReturn(Optional.of(order));
		when(cookieWalletRepository.findByUserIdWithLock(1L)).thenReturn(Optional.of(wallet));

		payActionWebhookService.handleMatchedWebhook(
			"webhook-key",
			"mall-id",
			"trace-1",
			new PayActionMatchedWebhookRequest("CK20260502000001", "매칭완료", "2026-05-02T15:32:00+09:00")
		);

		assertThat(order.isPaid()).isTrue();
		assertThat(order.getPayactionProcessingDate()).isEqualTo(LocalDateTime.of(2026, 5, 2, 15, 32));
		assertThat(wallet.getBalance()).isEqualTo(105);
		assertThat(user.getCookieBalance()).isEqualTo(105L);

		ArgumentCaptor<CookieTransaction> transactionCaptor = ArgumentCaptor.forClass(CookieTransaction.class);
		verify(cookieTransactionRepository).save(transactionCaptor.capture());
		assertThat(transactionCaptor.getValue().getAmount()).isEqualTo(100);
	}

	@Test
	void handleMatchedWebhookIsIdempotentWhenAlreadyPaid() {
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
		when(cookieOrderRepository.findByOrderNumberWithLock("CK20260502000001")).thenReturn(Optional.of(order));

		payActionWebhookService.handleMatchedWebhook(
			"webhook-key",
			"mall-id",
			"trace-1",
			new PayActionMatchedWebhookRequest("CK20260502000001", "매칭완료", "2026-05-02T15:32:00+09:00")
		);

		verify(cookieWalletRepository, never()).findByUserIdWithLock(any());
		verify(cookieTransactionRepository, never()).save(any());
	}

	@Test
	void handleMatchedWebhookOnlyChargesOnceWhenSameWebhookArrivesTwice() {
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
		CookieWallet wallet = CookieWallet.create(user, 0);
		when(cookieOrderRepository.findByOrderNumberWithLock("CK20260502000001")).thenReturn(Optional.of(order));
		when(cookieWalletRepository.findByUserIdWithLock(1L)).thenReturn(Optional.of(wallet));

		PayActionMatchedWebhookRequest request =
			new PayActionMatchedWebhookRequest("CK20260502000001", "매칭완료", "2026-05-02T15:32:00+09:00");
		payActionWebhookService.handleMatchedWebhook("webhook-key", "mall-id", "trace-1", request);
		payActionWebhookService.handleMatchedWebhook("webhook-key", "mall-id", "trace-2", request);

		assertThat(wallet.getBalance()).isEqualTo(100);
		assertThat(user.getCookieBalance()).isEqualTo(100L);
		verify(cookieTransactionRepository, times(1)).save(any());
	}

	private User createUser(Long id, Long cookies) {
		User user = User.create(1234L);
		ReflectionTestUtils.setField(user, "id", id);
		user.setPublicId("U0001");
		user.setCookies(cookies);
		return user;
	}
}
