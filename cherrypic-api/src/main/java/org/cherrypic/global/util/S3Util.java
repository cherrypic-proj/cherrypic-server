package org.cherrypic.global.util;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.Headers;
import com.amazonaws.services.s3.model.*;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.image.enums.BucketType;
import org.cherrypic.domain.image.enums.FileExtension;
import org.cherrypic.domain.image.enums.ImageType;
import org.cherrypic.helper.SpringEnvironmentHelper;
import org.cherrypic.s3.S3Properties;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class S3Util {

    private final SpringEnvironmentHelper springEnvironmentHelper;
    private final AmazonS3 amazonS3;
    private final S3Properties s3Properties;

    public String createPresignedUrl(
            BucketType bucketType,
            ImageType imageType,
            Long targetId,
            FileExtension fileExtension,
            String md5Hash) {
        String imageKey = UUID.randomUUID().toString();
        String fileName = createFileName(imageType, targetId, imageKey, fileExtension);
        String bucket =
                (bucketType == BucketType.MAIN)
                        ? s3Properties.mainBucket()
                        : s3Properties.tempAlbumBucket();

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

    public void deleteAllByUrls(BucketType bucketType, List<String> urls) {
        String bucket =
                (bucketType == BucketType.MAIN)
                        ? s3Properties.mainBucket()
                        : s3Properties.tempAlbumBucket();
        List<DeleteObjectsRequest.KeyVersion> keys =
                urls.stream()
                        .map(url -> extractObjectKey(bucketType, url))
                        .map(DeleteObjectsRequest.KeyVersion::new)
                        .toList();

        DeleteObjectsRequest request = new DeleteObjectsRequest(bucket).withKeys(keys);
        amazonS3.deleteObjects(request);
    }

    public void deleteAllByImageTypeAndTargetId(
            BucketType bucketType, ImageType imageType, Long targetId) {
        String bucket =
                (bucketType == BucketType.MAIN)
                        ? s3Properties.mainBucket()
                        : s3Properties.tempAlbumBucket();
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

    public void deleteByUrl(BucketType bucketType, String url) {
        String bucket =
                (bucketType == BucketType.MAIN)
                        ? s3Properties.mainBucket()
                        : s3Properties.tempAlbumBucket();
        String objectKey = extractObjectKey(bucketType, url);
        amazonS3.deleteObject(bucket, objectKey);
    }

    private String extractObjectKey(BucketType bucketType, String url) {
        String bucket =
                (bucketType == BucketType.MAIN)
                        ? s3Properties.mainBucket()
                        : s3Properties.tempAlbumBucket();
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
}
