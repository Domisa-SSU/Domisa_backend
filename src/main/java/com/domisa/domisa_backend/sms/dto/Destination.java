package com.domisa.domisa_backend.sms.dto;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Destination {

	private String to;
	private Map<String, String> replaceWords;

	public Destination(String to) {
		this.to = to;
		this.replaceWords = null;
	}
}
