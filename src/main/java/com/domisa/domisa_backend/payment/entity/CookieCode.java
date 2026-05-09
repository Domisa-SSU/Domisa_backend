package com.domisa.domisa_backend.payment.entity;

import com.domisa.domisa_backend.global.exception.GlobalErrorCode;
import com.domisa.domisa_backend.global.exception.GlobalException;
import java.util.Arrays;
import lombok.Getter;

@Getter
public enum CookieCode {

    COOKIE_5(5, 2_000),
    COOKIE_10(10, 4_000),
    COOKIE_30(30, 5_000),
    COOKIE_60(60, 9_000),
    // 테스트용. 운영 배포 전 제거하거나 dev/test에서만 허용해야 함.
    COOKIE_1000_TEST(1000, 1);

    private final int cookieAmount;
    private final int orderAmount;

    CookieCode(int cookieAmount, int orderAmount) {
        this.cookieAmount = cookieAmount;
        this.orderAmount = orderAmount;
    }

    public static CookieCode fromProductCode(String productCode) {
        if (productCode == null || productCode.isBlank()) {
            throw new GlobalException(GlobalErrorCode.MISSING_REQUIRED_FIELD);
        }

        return Arrays.stream(values())
                .filter(product -> product.name().equals(productCode))
                .findFirst()
                .orElseThrow(() -> new GlobalException(GlobalErrorCode.INVALID_COOKIE_PRODUCT));
    }
}