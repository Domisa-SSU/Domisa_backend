package com.domisa.domisa_backend.payment.service;

import com.domisa.domisa_backend.global.exception.GlobalErrorCode;
import com.domisa.domisa_backend.global.exception.GlobalException;
import com.domisa.domisa_backend.payment.entity.CookieCode;
import com.domisa.domisa_backend.payment.entity.CookieOrder;
import com.domisa.domisa_backend.payment.entity.OrderStatus;
import com.domisa.domisa_backend.payment.repository.CookieOrderRepository;
import com.domisa.domisa_backend.user.entity.User;
import com.domisa.domisa_backend.user.repository.UserRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CookieOrderTxService {

    private static final int MAX_BILLING_NAME_ATTEMPTS = 20;

    private final CookieOrderRepository cookieOrderRepository;
    private final UserRepository userRepository;
    private final OrderNumberGenerator orderNumberGenerator;
    private final BillingNameGenerator billingNameGenerator;

    @Transactional
    public CookieOrder createPendingOrder(Long userId, CookieCode cookieCode, LocalDateTime orderDate) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(GlobalErrorCode.USER_NOT_FOUND));

        String orderNumber = orderNumberGenerator.generate(orderDate);
        String billingName = generateUniqueBillingName();
        String ordererName = resolveOrdererName(user);

        CookieOrder order = CookieOrder.create(
                user,
                orderNumber,
                billingName,
                cookieCode.getOrderAmount(),
                cookieCode.getCookieAmount(),
                ordererName,
                orderDate
        );

        return cookieOrderRepository.save(order);
    }

    @Transactional
    public void markFailed(Long orderId) {
        CookieOrder order = cookieOrderRepository.findById(orderId)
                .orElseThrow(() -> new GlobalException(GlobalErrorCode.COOKIE_ORDER_NOT_FOUND));

        order.markFailed();
    }

    private String generateUniqueBillingName() {
        for (int attempt = 0; attempt < MAX_BILLING_NAME_ATTEMPTS; attempt++) {
            String billingName = billingNameGenerator.generate();

            boolean existsPending = cookieOrderRepository.existsByBillingNameAndStatus(
                    billingName,
                    OrderStatus.PENDING
            );

            if (!existsPending) {
                return billingName;
            }
        }

        throw new GlobalException(
                GlobalErrorCode.PAYACTION_ORDER_CREATE_FAILED,
                "입금자명 생성에 실패했습니다."
        );
    }

    private String resolveOrdererName(User user) {
        if (user.getName() != null && !user.getName().isBlank()) {
            return user.getName();
        }

        if (user.getNickname() != null && !user.getNickname().isBlank()) {
            return user.getNickname();
        }

        if (user.getPublicId() != null && !user.getPublicId().isBlank()) {
            return user.getPublicId();
        }

        return "UNKNOWN_USER";
    }
}
