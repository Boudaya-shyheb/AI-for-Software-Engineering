package com.aicareeragent.controller;

import com.aicareeragent.repository.FavoriteOfferRepository;
import com.aicareeragent.repository.JobOfferRepository;
import com.aicareeragent.repository.UserRepository;
import com.aicareeragent.model.FavoriteOffer;
import java.util.List;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/favorites")
public class FavoriteOfferController {
    private final FavoriteOfferRepository favorites; private final UserRepository users; private final JobOfferRepository offers;
    public FavoriteOfferController(FavoriteOfferRepository favorites, UserRepository users, JobOfferRepository offers) { this.favorites=favorites; this.users=users; this.offers=offers; }
    @GetMapping public List<Long> list(Authentication auth) { return favorites.findForUser(auth.getName()).stream().map(value -> value.getJobOffer().getId()).toList(); }
    @PostMapping("/{offerId}") public Map<String,Object> toggle(@PathVariable Long offerId, Authentication auth) {
        var user=users.findByEmailIgnoreCase(auth.getName()).orElseThrow();
        var existing=favorites.findByUserIdAndJobOfferId(user.getId(),offerId);
        if (existing.isPresent()) { favorites.delete(existing.get()); return Map.of("favorite",false,"jobOfferId",offerId); }
        var offer=offers.findById(offerId).filter(com.aicareeragent.model.JobOffer::isActive).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Job offer not found"));
        favorites.save(new FavoriteOffer(user,offer)); return Map.of("favorite",true,"jobOfferId",offerId);
    }
}
