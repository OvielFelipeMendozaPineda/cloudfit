package com.masabi.cloudfit.storage

import com.masabi.cloudfit.config.S3Config
import java.util.UUID
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest

class S3ImageStore(private val config: S3Config) : ImageStore {

    private val client = S3Client.builder()
        .region(Region.of(config.region))
        .build()

    override fun put(bytes: ByteArray, contentType: String): String {
        val ext = when (contentType) {
            "image/jpeg" -> "jpg"
            "image/webp" -> "webp"
            else -> "png"
        }
        val key = "cloudfit-${UUID.randomUUID()}.$ext"
        client.putObject(
            PutObjectRequest.builder()
                .bucket(config.bucket)
                .key(key)
                .contentType(contentType)
                .build(),
            RequestBody.fromBytes(bytes),
        )
        return "https://${config.bucket}.s3.${config.region}.amazonaws.com/$key"
    }

    override fun get(url: String): ByteArray {
        val key = url.substringAfterLast("/")
        return client.getObject(
            GetObjectRequest.builder().bucket(config.bucket).key(key).build(),
        ).readAllBytes()
    }
}
