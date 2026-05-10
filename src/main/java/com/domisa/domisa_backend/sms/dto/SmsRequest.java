package com.domisa.domisa_backend.sms.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SmsRequest {

	private List<Destination> destinations;
	private List<MessageFlow> messageFlow;
}
