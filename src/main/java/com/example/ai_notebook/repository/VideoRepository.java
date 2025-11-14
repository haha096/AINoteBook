package com.example.ai_notebook.repository;

import com.example.ai_notebook.entity.VideoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

// JpaRepository<[관리할 엔티티], [엔티티의 ID 타입]>
public interface VideoRepository extends JpaRepository<VideoEntity, Long> {

    /**
     * YouTube 고유 ID로 비디오 정보를 찾습니다.
     * (이미 DB에 저장된 영상인지 확인하는 데 필수적입니다.)
     */
    Optional<VideoEntity> findByYoutubeId(String youtubeId);

    // (필요에 따라 다른 조회 메서드 추가 가능. 예: findByCreatedAtDesc())
}
