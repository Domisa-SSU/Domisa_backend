package com.domisa.domisa_backend.global.s3.service;

import com.domisa.domisa_backend.global.s3.config.S3Properties;
import com.domisa.domisa_backend.global.s3.exception.S3ErrorCode;
import com.domisa.domisa_backend.global.s3.exception.S3Exception;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
public class S3ObjectStorageService {

	private static final String JPEG_CONTENT_TYPE = "image/jpeg";

	private final S3Client s3Client;
	private final S3Properties s3Properties;

	public boolean exists(String objectKey) {
		// 업로드 완료 여부 확인에 사용한다.
		try {
			s3Client.headObject(HeadObjectRequest.builder()
				.bucket(s3Properties.bucket())
				.key(objectKey)
				.build());
			return true;
		} catch (NoSuchKeyException exception) {
			return false;
		} catch (software.amazon.awssdk.services.s3.model.S3Exception exception) {
			if (exception.statusCode() == 404) {
				return false;
			}
			throw new S3Exception(S3ErrorCode.OBJECT_DOWNLOAD_FAILED, exception);
		} catch (SdkException exception) {
			throw new S3Exception(S3ErrorCode.OBJECT_DOWNLOAD_FAILED, exception);
		}
	}

	public byte[] read(String objectKey) {
		// 프로필 이미지 가공 전에 source 객체를 내려받는다.
		try {
			ResponseBytes<?> responseBytes = s3Client.getObjectAsBytes(GetObjectRequest.builder()
				.bucket(s3Properties.bucket())
				.key(objectKey)
				.build());
			return responseBytes.asByteArray();
		} catch (SdkException exception) {
			throw new S3Exception(S3ErrorCode.OBJECT_DOWNLOAD_FAILED, exception);
		}
	}

	public void uploadJpeg(String objectKey, byte[] bytes) {
		// 프로필 이미지 파생본은 현재 모두 JPG로 저장한다.
		try {
			s3Client.putObject(
				PutObjectRequest.builder()
					.bucket(s3Properties.bucket())
					.key(objectKey)
					.contentType(JPEG_CONTENT_TYPE)
					.build(),
				RequestBody.fromBytes(bytes)
			);
		} catch (SdkException exception) {
			throw new S3Exception(S3ErrorCode.OBJECT_UPLOAD_FAILED, exception);
		}
	}

	public void deleteAll(Collection<String> objectKeys) {
		// 같은 키가 중복으로 들어와도 한 번만 삭제한다.
		Set<String> uniqueKeys = new LinkedHashSet<>();
		for (String objectKey : objectKeys) {
			if (objectKey != null && !objectKey.isBlank()) {
				uniqueKeys.add(objectKey);
			}
		}

		for (String objectKey : uniqueKeys) {
			try {
				s3Client.deleteObject(DeleteObjectRequest.builder()
					.bucket(s3Properties.bucket())
					.key(objectKey)
					.build());
			} catch (SdkException exception) {
				throw new S3Exception(S3ErrorCode.OBJECT_DELETE_FAILED, exception);
			}
		}
	}
}
