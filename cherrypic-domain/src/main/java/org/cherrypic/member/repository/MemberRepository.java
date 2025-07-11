package org.cherrypic.member.repository;

import java.util.Optional;
import org.cherrypic.member.entity.Member;
import org.cherrypic.member.entity.OauthInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByOauthInfo(OauthInfo oauthInfo);
}
