package org.cherrypic.post.repository;

import org.cherrypic.post.entity.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostImageRepository extends JpaRepository<Long, PostImage> {}
