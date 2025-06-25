package com.example.ootd.domain.feed.repository;

import com.example.ootd.domain.feed.entity.Feed;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedRepository extends JpaRepository<Feed, UUID> {

}
