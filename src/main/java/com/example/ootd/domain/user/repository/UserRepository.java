package com.example.ootd.domain.user.repository;

import com.example.ootd.domain.user.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID>, CustomUserRepository {

  Optional<User> findByEmail(String email);

  @Query("SELECT u FROM User u LEFT JOIN FETCH u.location LEFT JOIN FETCH u.image WHERE u.id = :userId")
  Optional<User> findByIdWithLocationAndImage(UUID userId); // TODO : 이후 성능 최적화 -> Join 2개..

  @Query("select u.id from User u "
      + "where (:lastId is null or u.id > :lastId) "
      + "order by u.id asc")
  List<UUID> findIdsAfter(@Param("lastId") UUID lastId, Pageable pageable);

  @Query(value = "SELECT u.id FROM users u " +
               "LEFT JOIN locations l ON u.location_id = l.id " +
               "WHERE (',' || l.location_names || ',') LIKE CONCAT('%,', :city, ',%') " +
               "AND (',' || l.location_names || ',') LIKE CONCAT('%,', :district, ',%')",
         nativeQuery = true)
  List<UUID> findUserIdsByRegion(
      @Param("city") String city,
      @Param("district") String district);

}
