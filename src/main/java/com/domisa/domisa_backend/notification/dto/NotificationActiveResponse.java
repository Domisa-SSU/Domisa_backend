package com.domisa.domisa_backend.notification.dto;

public record NotificationActiveResponse(
	boolean signup,
	long referralCount,
	boolean like,
	boolean match
) {
}
