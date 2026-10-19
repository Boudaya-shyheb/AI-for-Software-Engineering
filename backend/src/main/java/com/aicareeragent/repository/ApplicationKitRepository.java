package com.aicareeragent.repository;

import com.aicareeragent.model.ApplicationKit;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApplicationKitRepository extends JpaRepository<ApplicationKit, Long> {
    @Query("""
            select kit from ApplicationKit kit
            join fetch kit.jobOffer
            join fetch kit.user
            where lower(kit.user.email) = lower(:email)
            order by kit.createdAt desc
            """)
    List<ApplicationKit> findAllByOwnerEmail(@Param("email") String email);

    @Query("""
            select kit from ApplicationKit kit
            join fetch kit.jobOffer
            join fetch kit.user
            where kit.id = :id and lower(kit.user.email) = lower(:email)
            """)
    Optional<ApplicationKit> findByIdAndOwnerEmail(@Param("id") Long id, @Param("email") String email);

    boolean existsByJobOfferId(Long jobOfferId);
}
