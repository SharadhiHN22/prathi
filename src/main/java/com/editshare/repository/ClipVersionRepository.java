package com.editshare.repository;

import com.editshare.entity.ClipVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClipVersionRepository extends JpaRepository<ClipVersion, Long> {

    @Query("SELECT cv FROM ClipVersion cv WHERE cv.clip.id = :clipId ORDER BY cv.versionNumber DESC")
    List<ClipVersion> findByClipIdOrderByVersionNumberDesc(@Param("clipId") String clipId);

    @Query("SELECT cv FROM ClipVersion cv WHERE cv.clip.id = :clipId AND cv.versionNumber = :versionNumber")
    Optional<ClipVersion> findByClipIdAndVersionNumber(@Param("clipId") String clipId, @Param("versionNumber") Integer versionNumber);

    @Modifying
    @Query("DELETE FROM ClipVersion cv WHERE cv.clip.id = :clipId")
    void deleteByClipId(@Param("clipId") String clipId);
}
