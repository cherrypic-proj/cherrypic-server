package org.cherrypic.domain.image.service;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.Headers;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.cherrypic.domain.image.dto.request.MemberProfileImageUploadRequest;
import org.cherrypic.domain.image.dto.response.PresignedUrlResponse;
import org.cherrypic.domain.image.enums.ImageFileExtension;
import org.cherrypic.domain.image.enums.ImageType;
import org.cherrypic.global.util.MemberUtil;
import org.cherrypic.helper.SpringEnvironmentHelper;
import org.cherrypic.member.entity.Member;
import org.cherrypic.s3.S3Properties;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {

    private final MemberUtil memberUtil;
    private final SpringEnvironmentHelper springEnvironmentHelper;
    private final AmazonS3 amazonS3;
    private final S3Properties s3Properties;

    @Override
    public PresignedUrlResponse createMemberProfileImageUploadUrl(
            MemberProfileImageUploadRequest request) {
        final Member currentMember = memberUtil.getCurrentMember();

        String imageKey = generateUUID();
        String fileName =
                createFileName(
                        ImageType.MEMBER_PROFILE,
                        currentMember.getId(),
                        imageKey,
                        request.imageFileExtension());
        GeneratePresignedUrlRequest generatePresignedUrlRequest =
                generatePresignedUrlRequest(
                        s3Properties.bucket(),
                        fileName,
                        request.imageFileExtension().getExtension());

        String presignedUrl = amazonS3.generatePresignedUrl(generatePresignedUrlRequest).toString();

        return new PresignedUrlResponse(presignedUrl);
    }

    private String generateUUID() {
        return UUID.randomUUID().toString();
    }

    private String createFileName(
            ImageType imageType,
            Long targetId,
            String imageKey,
            ImageFileExtension imageFileExtension) {
        return springEnvironmentHelper.getCurrentProfile()
                + "/"
                + imageType.getType()
                + "/"
                + targetId
                + "/"
                + imageKey
                + "."
                + imageFileExtension.getExtension();
    }

    private GeneratePresignedUrlRequest generatePresignedUrlRequest(
            String bucket, String fileName, String imageFileExtension) {
        GeneratePresignedUrlRequest generatePresignedUrlRequest =
                new GeneratePresignedUrlRequest(bucket, fileName, HttpMethod.PUT)
                        .withKey(fileName)
                        .withContentType("image/" + imageFileExtension)
                        .withExpiration(getPresignedUrlExpiration());

        generatePresignedUrlRequest.addRequestParameter(
                Headers.S3_CANNED_ACL, CannedAccessControlList.PublicRead.toString());

        return generatePresignedUrlRequest;
    }

    private Date getPresignedUrlExpiration() {
        Date expiration = new Date();
        long expTimeMillis = expiration.getTime();
        expTimeMillis += TimeUnit.MINUTES.toMillis(1);
        expiration.setTime(expTimeMillis);

        return expiration;
    }
}
