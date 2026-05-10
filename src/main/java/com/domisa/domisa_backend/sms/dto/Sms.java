package com.domisa.domisa_backend.sms.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Sms {

	private String from;
	private String text;
}
