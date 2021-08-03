package com.ddkolesnik.siteparser.service;

import com.ddkolesnik.siteparser.model.Advertisement;
import com.ddkolesnik.siteparser.repository.AdvertisementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @author Alexandr Stegnin
 */

@Service
@Transactional
public class AdvertisementService {

    private final AdvertisementRepository advertisementRepository;

    public AdvertisementService(AdvertisementRepository advertisementRepository) {
        this.advertisementRepository = advertisementRepository;
    }

    @Transactional
    public void create(Advertisement advertisement) {
        advertisementRepository.save(advertisement);
    }

    public long count() {
        return advertisementRepository.count();
    }

    public LocalDate getMaxPublishDate() {
        return advertisementRepository.getMaxPublishDate();
    }

    public void setNotActual(LocalDateTime currentDate) {
        advertisementRepository.setNotActual(currentDate);
    }

    public void delete(LocalDateTime currentDate) {
        advertisementRepository.deleteByCreationTimeBefore(currentDate);
    }

}
