package com.aicareeragent.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aicareeragent.model.JobOffer;
import com.aicareeragent.repository.JobOfferRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.core.io.DefaultResourceLoader;

class JobOfferDatasetSeederTest {
    @Test
    void seedsOneThousandFictionalOffersAcrossDomainsAndIsIdempotent() {
        JobOfferRepository repository = mock(JobOfferRepository.class);
        List<JobOffer> persistedOffers = new ArrayList<>();
        when(repository.findAll()).thenAnswer(invocation -> new ArrayList<>(persistedOffers));
        when(repository.saveAll(any())).thenAnswer(invocation -> {
            Iterable<JobOffer> inserted = invocation.getArgument(0);
            List<JobOffer> batch = new ArrayList<>();
            inserted.forEach(batch::add);
            persistedOffers.addAll(batch);
            return batch;
        });
        JobOfferDatasetSeeder seeder = new JobOfferDatasetSeeder(repository, new ObjectMapper(), new DefaultResourceLoader());

        seeder.run(new DefaultApplicationArguments(new String[0]));
        seeder.run(new DefaultApplicationArguments(new String[0]));

        assertEquals(1000, persistedOffers.size());
        assertEquals(1000, persistedOffers.stream().map(offer -> offer.getTitle() + "|" + offer.getCompany()).distinct().count());
        Set<String> domains = new HashSet<>();
        for (JobOffer offer : persistedOffers) {
            assertTrue(offer.getDescription().startsWith("Fictional CareerLens demo listing"));
            String domainLine = offer.getDescription().lines().filter(line -> line.startsWith("Domain: ")).findFirst().orElseThrow();
            domains.add(domainLine.substring("Domain: ".length()));
        }
        assertEquals(22, domains.size());
        verify(repository, times(1)).saveAll(any());
    }
}
