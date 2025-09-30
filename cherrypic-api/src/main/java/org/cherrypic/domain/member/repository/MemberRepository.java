package org.cherrypic.domain.member.repository;

import java.util.Optional;
import org.cherrypic.member.entity.Member;
import org.cherrypic.member.entity.OauthInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByOauthInfo(OauthInfo oauthInfo);

    @Modifying
    @Query("delete from Member m where m.nickname = :nickname")
    void deleteByNickname(@Param("nickname") String nickname);
}
