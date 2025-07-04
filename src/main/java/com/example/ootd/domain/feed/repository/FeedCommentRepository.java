package com.example.ootd.domain.feed.repository;

import com.example.ootd.domain.feed.entity.FeedComment;
import com.example.ootd.domain.feed.repository.custom.CustomFeedCommentRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FeedCommentRepository extends JpaRepository<FeedComment, UUID>,
    CustomFeedCommentRepository {

  @Query("""
      SELECT count(fc.id)
        FROM FeedComment fc
       WHERE fc.feed.id = :feedId
      """)
  long countByFeedId(@Param("feedId") UUID feedId);
}
