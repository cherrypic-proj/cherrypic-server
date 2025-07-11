package org.cherrypic.domain.auth.util;

import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

@Component
public class NicknameGenerator {

    private static final String[] PREFIX_NAMES = {
        "귀여운", "졸린", "행복한", "멋진", "배고픈",
        "활발한", "조용한", "용감한", "상냥한", "차분한",
        "명랑한", "신나는", "엉뚱한", "불타는", "우아한",
        "재빠른", "신비한", "웃긴", "똑똑한", "천재적인",
        "수줍은", "반짝이는", "느긋한", "예민한", "호기심많은"
    };

    private static final String[] ANIMAL_NAMES = {
        "사자", "호랑이", "토끼", "펭귄", "너구리",
        "고양이", "강아지", "코끼리", "기린", "판다",
        "올빼미", "햄스터", "하마", "여우", "고슴도치",
        "수달", "공작", "앵무새", "두더지", "독수리",
        "타조", "다람쥐", "카피바라", "알파카", "라마",
        "북극곰", "늑대", "퓨마", "이구아나", "돌고래"
    };

    public String generateNickname() {
        int animalIndex = ThreadLocalRandom.current().nextInt(ANIMAL_NAMES.length);
        int prefixIndex = ThreadLocalRandom.current().nextInt(PREFIX_NAMES.length);

        return PREFIX_NAMES[prefixIndex] + " " + ANIMAL_NAMES[animalIndex];
    }
}
