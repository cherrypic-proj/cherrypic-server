package org.cherrypic.domain.auth.repository;

import org.cherrypic.auth.entity.RefreshToken;
import org.springframework.data.repository.CrudRepository;

public interface RefreshTokenRepository extends CrudRepository<RefreshToken, Long> {}
