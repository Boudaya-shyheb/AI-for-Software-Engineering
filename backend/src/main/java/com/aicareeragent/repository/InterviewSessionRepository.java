package com.aicareeragent.repository;

import com.aicareeragent.model.InterviewSession;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Long> {
    @EntityGraph(attributePaths = "user")
    List<InterviewSession> findAllByOrderByCreatedAtDesc();
    List<InterviewSession> findAllByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<InterviewSession> findByIdAndUserId(Long id, Long userId);
}
