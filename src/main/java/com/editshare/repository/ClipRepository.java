package com.editshare.repository;

import com.editshare.entity.Clip;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClipRepository extends JpaRepository<Clip, String> {

    @Query("SELECT c FROM Clip c LEFT JOIN FETCH c.versions WHERE c.id = :id OR c.slug = :id")
    Optional<Clip> findByIdWithVersions(@Param("id") String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Clip c WHERE c.id = :id OR c.slug = :id")
    Optional<Clip> findByIdForUpdate(@Param("id") String id);

    @Query("SELECT c FROM Clip c WHERE c.id = :identifier OR c.slug = :identifier")
    Optional<Clip> findByIdOrSlug(@Param("identifier") String identifier);

    boolean existsBySlug(String slug);

    Optional<Clip> findBySlug(String slug);

    @Modifying
    @Query("DELETE FROM Clip c WHERE c.expiresAt IS NOT NULL AND c.expiresAt <= :now")
    int deleteExpiredClips(@Param("now") LocalDateTime now);

    List<Clip> findByExpiresAtNotNullAndExpiresAtLessThanEqual(LocalDateTime now);
}
