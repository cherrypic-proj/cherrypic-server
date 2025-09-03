package org.cherrypic.album.entity.subscription;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.LocalDateTime;
import org.cherrypic.subscription.entity.Subscription;
import org.junit.jupiter.api.Test;

class SubscriptionTest {

    @Test
    void 결제일이_28일_이상이면_첫_종료일은_다음_달의_말일로_보정된다() {
        // given
        LocalDateTime paidAt = LocalDateTime.of(2025, 1, 31, 13, 0);

        // when
        LocalDateTime adjusted = Subscription.adjustEndDate(paidAt);

        // then
        assertThat(adjusted).isEqualTo(LocalDateTime.of(2025, 2, 28, 13, 0));
    }

    @Test
    void 구독_종료일이_28일_이상이면_갱신된_종료일도_다음_달의_말일로_보정된다() {
        // given
        LocalDateTime endAt = LocalDateTime.of(2025, 2, 28, 13, 0);

        // when
        LocalDateTime adjusted = Subscription.adjustEndDate(endAt);

        // then
        assertThat(adjusted).isEqualTo(LocalDateTime.of(2025, 3, 31, 13, 0));
    }
}
