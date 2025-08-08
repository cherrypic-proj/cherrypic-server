package org.cherrypic.domain.member.repository;

import java.util.Optional;
import org.cherrypic.member.entity.Member;
import org.cherrypic.member.entity.OauthInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByOauthInfo(OauthInfo oauthInfo);

    @Query("select m.nickname from Member m where m.id = :id")
    String findNicknameById(@Param("id") Long id);
}
