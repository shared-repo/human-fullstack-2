package com.example.imageboard.repository;

import com.example.imageboard.entity.AttachedImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttachedImageRepository extends JpaRepository<AttachedImage, Long> {
}
