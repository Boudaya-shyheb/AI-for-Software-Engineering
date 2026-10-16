package com.aicareeragent.config;

import com.aicareeragent.model.JobOffer;
import com.aicareeragent.repository.JobOfferRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(prefix = "app.job-offers.seed", name = "enabled", havingValue = "true")
public class JobOfferDatasetSeeder implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(JobOfferDatasetSeeder.class);
    private static final int EXPECTED_DATASET_SIZE = 1000;
    private static final String DATASET_PATH = "classpath:data/job-offers.json";

    private final JobOfferRepository offers;
    private final ObjectMapper mapper;
    private final ResourceLoader resources;

    public JobOfferDatasetSeeder(JobOfferRepository offers, ObjectMapper mapper, ResourceLoader resources) {
        this.offers = offers;
        this.mapper = mapper;
        this.resources = resources;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Resource datasetResource = resources.getResource(DATASET_PATH);
        List<SeedOffer> dataset;
        try (InputStream input = datasetResource.getInputStream()) {
            dataset = mapper.readValue(input, new TypeReference<>() {});
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load the CareerLens job-offer dataset", exception);
        }
        if (dataset.size() != EXPECTED_DATASET_SIZE) {
            throw new IllegalStateException("Expected " + EXPECTED_DATASET_SIZE + " sample job offers, found " + dataset.size());
        }

        Set<String> knownOffers = new HashSet<>();
        offers.findAll().forEach(offer -> knownOffers.add(key(offer.getTitle(), offer.getCompany())));
        Set<String> datasetKeys = new HashSet<>();
        List<JobOffer> missingOffers = new ArrayList<>();
        for (SeedOffer offer : dataset) {
            validate(offer);
            String key = key(offer.title(), offer.company());
            if (!datasetKeys.add(key)) {
                throw new IllegalStateException("Duplicate title and company in job-offer dataset: " + offer.title() + " / " + offer.company());
            }
            if (knownOffers.add(key)) {
                missingOffers.add(new JobOffer(offer.title(), offer.company(), offer.location(), offer.employmentType(), offer.description()));
            }
        }

        if (!missingOffers.isEmpty()) {
            offers.saveAll(missingOffers);
        }
        logger.info("Job-offer demo dataset checked: {} entries, {} inserted", dataset.size(), missingOffers.size());
    }

    private static void validate(SeedOffer offer) {
        if (offer == null || blank(offer.title()) || blank(offer.company()) || blank(offer.location())
                || blank(offer.employmentType()) || blank(offer.description())) {
            throw new IllegalStateException("Job-offer dataset contains an incomplete entry");
        }
        if (offer.title().length() > 160 || offer.company().length() > 140 || offer.location().length() > 140
                || offer.employmentType().length() > 80 || offer.description().length() > 20000) {
            throw new IllegalStateException("Job-offer dataset contains an entry exceeding database limits: " + offer.title());
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static String key(String title, String company) {
        return title.trim().toLowerCase(Locale.ROOT) + "|" + company.trim().toLowerCase(Locale.ROOT);
    }

    private record SeedOffer(String title, String company, String location, String employmentType, String description) {}
}
