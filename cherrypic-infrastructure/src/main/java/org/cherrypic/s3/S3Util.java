package org.cherrypic.s3;

import com.amazonaws.HttpMethod;
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

        generatePresignedUrlRequest.setContentMd5(base64Md5);

        return generatePresignedUrlRequest;
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

    public void deleteAllTempAlbumImagesInBatch(List<Long> tempAlbumIds) {
        if (tempAlbumIds == null || tempAlbumIds.isEmpty()) {
            log.info("deleteAllTempAlbumImagesInBatch skipped: empty targetIds");
            return;
        }

        String bucket = s3Properties.bucket();
        ImageType imageType = ImageType.TEMP_ALBUM_IMAGE;

        for (Long targetId : tempAlbumIds) {
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
                    log.info(
                            "Deleted {} objects for targetId={} (TEMP_ALBUM_IMAGE)",
                            keys.size(),
                            targetId);
                } else {
                    log.info("No objects found for targetId={} (TEMP_ALBUM_IMAGE)", targetId);
                }

                listReq.setContinuationToken(result.getNextContinuationToken());
            } while (result.isTruncated());
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
