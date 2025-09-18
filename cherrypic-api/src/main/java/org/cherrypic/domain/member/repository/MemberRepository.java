package org.cherrypic.domain.member.repository;

import java.util.List;
import java.util.Optional;
import org.cherrypic.member.entity.Member;
import org.cherrypic.member.entity.OauthInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByOauthInfo(OauthInfo oauthInfo);

    @Query("SELECT m.id FROM Member m WHERE m.id IN :ids AND m.serviceAlarmAgree = true")
    List<Long> findServiceAlarmAgreedIds(List<Long> ids);
}
