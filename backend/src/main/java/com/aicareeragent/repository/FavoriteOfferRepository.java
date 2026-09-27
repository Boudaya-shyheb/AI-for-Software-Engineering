package com.aicareeragent.repository;

import com.aicareeragent.model.FavoriteOffer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FavoriteOfferRepository extends JpaRepository<FavoriteOffer, Long> {
    Optional<FavoriteOffer> findByUserIdAndJobOfferId(Long userId, Long offerId);
    boolean existsByJobOfferId(Long offerId);
    @Query("select favorite from FavoriteOffer favorite join fetch favorite.jobOffer where lower(favorite.user.email)=lower(:email) order by favorite.createdAt desc")
    List<FavoriteOffer> findForUser(@Param("email") String email);
}
