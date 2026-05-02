package com.domisa.domisa_backend.payment.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

@Component
public class OrderNumberGenerator {

	private static final DateTimeFormatter ORDER_DATE_PREFIX_FORMATTER = DateTimeFormatter.ofPattern("yyMMdd");

	public String generate(LocalDate orderDate, int sequence) {
		return prefix(orderDate) + String.format("%06d", sequence);
	}

	public String prefix(LocalDate orderDate) {
		return "ORD" + orderDate.format(ORDER_DATE_PREFIX_FORMATTER);
	}
}
