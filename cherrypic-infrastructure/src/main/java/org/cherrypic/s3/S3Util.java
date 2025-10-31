package org.cherrypic.s3;

import com.amazonaws.HttpMethod;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.Headers;
import com.amazonaws.services.s3.model.*;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cherrypic.helper.SpringEnvironmentHelper;
import org.cherrypic.s3.enums.FileExtension;
import org.cherrypic.s3.enums.ImageType;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class S3Util {

    private final SpringEnvironmentHelper springEnvironmentHelper;
    private final AmazonS3 amazonS3;
    private final S3Properties s3Properties;

    public String createPresignedUrl(
            ImageType imageType, Long targetId, FileExtension fileExtension, String md5Hash) {
        String imageKey = UUID.randomUUID().toString();
        String fileName = createFileName(imageType, targetId, imageKey, fileExtension);
        String bucket = s3Properties.bucket();

        GeneratePresignedUrlRequest generatePresignedUrlRequest =
                generatePresignedUrlRequest(
                        bucket, fileName, fileExtension.getExtension(), md5Hash);

        return amazonS3.generatePresignedUrl(generatePresignedUrlRequest).toString();
    }

    private String createFileName(
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

    private GeneratePresignedUrlRequest generatePresignedUrlRequest(
            String bucket, String fileName, String imageFileExtension, String base64Md5) {
        GeneratePresignedUrlRequest generatePresignedUrlRequest =
                new GeneratePresignedUrlRequest(bucket, fileName, HttpMethod.PUT)
                        .withKey(fileName)
                        .withContentType("image/" + imageFileExtension)
                        .withExpiration(getPresignedUrlExpiration());

        generatePresignedUrlRequest.addRequestParameter(
                Headers.S3_CANNED_ACL, CannedAccessControlList.PublicRead.toString());

        generatePresignedUrlRequest.addRequestParameter("x-amz-tagging", "status=pending");

        generatePresignedUrlRequest.setContentMd5(base64Md5);

        return generatePresignedUrlRequest;
    }

    public void updateTagToCompleteByUrl(String url) {
        String bucket = s3Properties.bucket();
        String key = extractObjectKey(url);

        List<Tag> tags = List.of(new Tag("status", "complete"));
        ObjectTagging tagging = new ObjectTagging(tags);

        SetObjectTaggingRequest request = new SetObjectTaggingRequest(bucket, key, tagging);
        amazonS3.setObjectTagging(request);
    }

    public boolean doesFileExistByUrl(String url) {
        String bucket = s3Properties.bucket();
        String key = extractObjectKey(url);
        try {
            return amazonS3.doesObjectExist(bucket, key);
        } catch (AmazonS3Exception e) {
            if (e.getStatusCode() == 403) {
                log.warn("Access denied for key={}, treating as non-existent", key);
                return false;
            }
            log.error("S3 error while checking existence: {}", e.getErrorMessage(), e);
            return false;
        } catch (SdkClientException e) {
            log.error("Network error while connecting to S3: {}", e.getMessage());
            return false;
        }
    }

    public void deleteAllByUrls(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            log.info("deleteAllByUrls skipped: received null or empty urls");
            return;
        }

        String bucket = s3Properties.bucket();

        List<DeleteObjectsRequest.KeyVersion> keys =
                urls.stream()
                        .map(this::extractObjectKey)
                        .map(DeleteObjectsRequest.KeyVersion::new)
                        .toList();

        DeleteObjectsRequest request = new DeleteObjectsRequest(bucket).withKeys(keys);
        amazonS3.deleteObjects(request);
    }

    public void deleteAllByImageTypeAndTargetId(ImageType imageType, Long targetId) {
        String bucket = s3Properties.bucket();
        String prefix =
                springEnvironmentHelper.getCurrentProfile()
                        + "/"
                        + imageType.getType()
                        + "/"
                        + targetId
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

    public void deleteAllTempAlbumImagesInBatch(List<Long> targetIds) {
        if (targetIds == null || targetIds.isEmpty()) {
            log.info("deleteAllByImageTypeAndTargetIds skipped: empty targetIds");
            return;
        }

        for (Long targetId : targetIds) {
            deleteAllByImageTypeAndTargetId(ImageType.TEMP_ALBUM_IMAGE, targetId);
        }
    }

    public void deleteByUrl(String url) {
        String bucket = s3Properties.bucket();
        String objectKey = extractObjectKey(url);
        amazonS3.deleteObject(bucket, objectKey);
    }

    private String extractObjectKey(String url) {
        int comIndex = url.indexOf(".com/");
        return url.substring(comIndex + 5);
    }

    private Date getPresignedUrlExpiration() {
        Date expiration = new Date();
        long expTimeMillis = expiration.getTime();
        expTimeMillis += TimeUnit.MINUTES.toMillis(1);
        expiration.setTime(expTimeMillis);

        return expiration;
    }
}
