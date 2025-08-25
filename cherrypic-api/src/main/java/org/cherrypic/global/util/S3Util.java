package org.cherrypic.global.util;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.Headers;
import com.amazonaws.services.s3.model.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.image.enums.FileExtension;
import org.cherrypic.domain.image.enums.ImageType;
import org.cherrypic.domain.tempalbum.dto.TempAlbumErrorCode;
import org.cherrypic.exception.CustomException;
import org.cherrypic.helper.SpringEnvironmentHelper;
import org.cherrypic.s3.S3Properties;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class S3Util {

    private final SpringEnvironmentHelper springEnvironmentHelper;
    private final AmazonS3 amazonS3;
    private final S3Properties s3Properties;

    public String createUploadPresignedUrl(
            ImageType imageType, Long targetId, FileExtension fileExtension, String md5Hash) {
        String imageKey = UUID.randomUUID().toString();
        String fileName = createUploadFileName(imageType, targetId, imageKey, fileExtension);

        GeneratePresignedUrlRequest generatePresignedUrlRequest =
                generateUploadPresignedUrlRequest(
                        s3Properties.mainBucket(), fileName, fileExtension.getExtension(), md5Hash);

        return amazonS3.generatePresignedUrl(generatePresignedUrlRequest).toString();
    }

    public String createReadOnlyPresignedUrl(String imageUrl, Date expirationDate) {

        String fileName = extractFileName(imageUrl);

        GeneratePresignedUrlRequest generatePresignedUrlRequest =
                generateReadOnlyPresignedUrlRequest(
                        s3Properties.mainBucket(), fileName, expirationDate);

        return amazonS3.generatePresignedUrl(generatePresignedUrlRequest).toString();
    }

    public void uploadTempAlbum(String bucket, String key, String htmlContent) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("text/html; charset=UTF-8");
        metadata.setContentLength(htmlContent.getBytes(StandardCharsets.UTF_8).length);

        try (InputStream inputStream =
                new ByteArrayInputStream(htmlContent.getBytes(StandardCharsets.UTF_8))) {
            amazonS3.putObject(new PutObjectRequest(bucket, key, inputStream, metadata));
        } catch (IOException e) {
            throw new CustomException(TempAlbumErrorCode.TEMP_ALBUM_UPLOAD_FAIL);
        }
    }

    private String createUploadFileName(
            ImageType imageType, Long targetId, String imageKey, FileExtension fileExtension) {
        return springEnvironmentHelper.getCurrentProfile()
                + "/"
                + imageType.getType()
                + "/"
                + targetId
                + "/"
                + imageKey
                + "."
                + fileExtension.getExtension();
    }

    private GeneratePresignedUrlRequest generateReadOnlyPresignedUrlRequest(
            String bucket, String fileName, Date expiration) {

        return new GeneratePresignedUrlRequest(bucket, fileName, HttpMethod.GET)
                .withKey(fileName)
                .withExpiration(expiration);
    }

    private GeneratePresignedUrlRequest generateUploadPresignedUrlRequest(
            String bucket, String fileName, String imageFileExtension, String md5Hash) {
        GeneratePresignedUrlRequest generatePresignedUrlRequest =
                new GeneratePresignedUrlRequest(bucket, fileName, HttpMethod.PUT)
                        .withKey(fileName)
                        .withContentType("image/" + imageFileExtension)
                        .withExpiration(getPresignedUrlExpiration());

        generatePresignedUrlRequest.addRequestParameter(
                Headers.S3_CANNED_ACL, CannedAccessControlList.PublicRead.toString());

        generatePresignedUrlRequest.addRequestParameter(Headers.CONTENT_MD5, md5Hash);

        return generatePresignedUrlRequest;
    }

    public void deleteFilesInBatchFromS3(List<String> urls) {
        String bucket = s3Properties.mainBucket();
        List<DeleteObjectsRequest.KeyVersion> keys =
                urls.stream()
                        .map(this::extractObjectKey)
                        .map(DeleteObjectsRequest.KeyVersion::new)
                        .toList();

        DeleteObjectsRequest request = new DeleteObjectsRequest(bucket).withKeys(keys);
        amazonS3.deleteObjects(request);
    }

    public void deleteAllAlbumImagesInBatchFromS3(Long albumId) {
        String bucket = s3Properties.mainBucket();
        String prefix =
                springEnvironmentHelper.getCurrentProfile()
                        + "/"
                        + ImageType.ALBUM_IMAGE.getType()
                        + "/"
                        + albumId
                        + "/";

        ListObjectsV2Request listReq =
                new ListObjectsV2Request().withBucketName(bucket).withPrefix(prefix);

        ListObjectsV2Result result;
        do {
            result = amazonS3.listObjectsV2(listReq);

            List<DeleteObjectsRequest.KeyVersion> keys =
                    result.getObjectSummaries().stream()
                            .map(S3ObjectSummary::getKey)
                            .map(DeleteObjectsRequest.KeyVersion::new)
                            .toList();

            if (!keys.isEmpty()) {
                amazonS3.deleteObjects(new DeleteObjectsRequest(bucket).withKeys(keys));
            }

            listReq.setContinuationToken(result.getNextContinuationToken());
        } while (result.isTruncated());
    }

    public void deleteFileFromS3(String url) {
        String bucket = s3Properties.mainBucket();
        String objectKey = extractObjectKey(url);
        amazonS3.deleteObject(bucket, objectKey);
    }

    private String extractObjectKey(String url) {
        String bucket = s3Properties.mainBucket();
        int idx = url.indexOf(bucket) + bucket.length() + 1;
        return url.substring(idx);
    }

    private Date getPresignedUrlExpiration() {
        Date expiration = new Date();
        long expTimeMillis = expiration.getTime();
        expTimeMillis += TimeUnit.MINUTES.toMillis(1);
        expiration.setTime(expTimeMillis);

        return expiration;
    }

    private String extractFileName(String url) {
        return url.substring(url.lastIndexOf("/") + 1);
    }
}
