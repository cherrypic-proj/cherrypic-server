package org.cherrypic.post.repository;

import org.cherrypic.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Long, Post> {}
