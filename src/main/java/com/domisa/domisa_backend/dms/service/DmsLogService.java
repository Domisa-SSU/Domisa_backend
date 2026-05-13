package com.domisa.domisa_backend.dms.service;

import com.domisa.domisa_backend.dms.dto.DmsLogResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DmsLogService {

	private static final int DEFAULT_LINES = 300;
	private static final int MAX_LINES = 500;
	private static final long MAX_READ_BYTES = 256 * 1024;

	@Value("${app.admin.log-file:${LOGGING_FILE_NAME:logs/domisa-backend.log}}")
	private String logFile;

	public DmsLogResponse emptyLogs(Integer requestedLines) {
		int lines = normalizeLines(requestedLines);
		Path path = Path.of(logFile).toAbsolutePath().normalize();
		return new DmsLogResponse(path.toString(), lines, "", null, false);
	}

	public DmsLogResponse getLogs(Integer requestedLines) {
		int lines = normalizeLines(requestedLines);
		Path path = Path.of(logFile).toAbsolutePath().normalize();
		if (!Files.exists(path)) {
			return new DmsLogResponse(path.toString(), lines, "", "로그 파일이 없습니다.", true);
		}
		if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
			return new DmsLogResponse(path.toString(), lines, "", "로그 파일을 읽을 수 없습니다.", true);
		}

		try {
			String content = readTail(path);
			return new DmsLogResponse(path.toString(), lines, lastLines(content, lines), null, true);
		} catch (IOException exception) {
			return new DmsLogResponse(path.toString(), lines, "", "로그 파일을 읽는 중 오류가 발생했습니다.", true);
		}
	}

	private int normalizeLines(Integer requestedLines) {
		if (requestedLines == null) {
			return DEFAULT_LINES;
		}
		return Math.max(1, Math.min(requestedLines, MAX_LINES));
	}

	private String readTail(Path path) throws IOException {
		long size = Files.size(path);
		long skip = Math.max(0, size - MAX_READ_BYTES);
		try (InputStream inputStream = Files.newInputStream(path)) {
			if (skip > 0) {
				inputStream.skipNBytes(skip);
			}
			return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private String lastLines(String content, int lines) {
		String[] split = content.split("\\R");
		if (split.length <= lines) {
			return content;
		}
		return String.join(System.lineSeparator(), Arrays.copyOfRange(split, split.length - lines, split.length));
	}
}
