package app.cloudfit.shared.infrastructure.storage

import app.cloudfit.shared.application.port.ImageContent
import app.cloudfit.shared.application.port.ImageFormats
import app.cloudfit.shared.application.port.ImageStore
import app.cloudfit.shared.application.port.StoredImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest

class S3ImageStore(
    private val bucket: String,
    private val region: String,
) : ImageStore {

    init {
        require(bucket.isNotBlank() && region.isNotBlank()) { "AWS_S3_BUCKET/AWS_REGION are required when IMAGE_STORE=s3" }
    }

    private val client = S3Client.builder().region(Region.of(region)).build()
    private val baseUrl = "https://$bucket.s3.$region.amazonaws.com"

    override suspend fun put(bytes: ByteArray, contentType: String): StoredImage = withContext(Dispatchers.IO) {
        val key = ImageFormats.newKey(contentType)
        client.putObject(
            PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType).build(),
            RequestBody.fromBytes(bytes),
        )
        StoredImage(key = key, url = "$baseUrl/$key")
    }

    override suspend fun read(url: String): ImageContent {
        require(url.startsWith("$baseUrl/")) { "URL $url does not belong to bucket $bucket" }
        return fetch(url.removePrefix("$baseUrl/"))
    }

    override suspend fun readByKey(key: String): ImageContent? = null

    override suspend fun delete(key: String) {
        withContext(Dispatchers.IO) {
            client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build())
        }
    }

    private suspend fun fetch(key: String): ImageContent = withContext(Dispatchers.IO) {
        val response = client.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build())
        ImageContent(response.readAllBytes(), response.response().contentType() ?: ImageFormats.contentTypeOf(key))
    }
}
