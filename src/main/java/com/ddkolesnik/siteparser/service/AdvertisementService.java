package com.ddkolesnik.siteparser.service;

import com.ddkolesnik.siteparser.model.Advertisement;
import com.ddkolesnik.siteparser.repository.AdvertisementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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

    public List<Advertisement> findAll() {
        return advertisementRepository.findAll();
    }

    public long count() {
        return advertisementRepository.count();
    }

    public void deleteAll() {
        advertisementRepository.deleteAll();
    }

    public void deleteOld(LocalDateTime currentDate) {
        advertisementRepository.deleteOld(currentDate);
    }

    public LocalDate getMaxPublishDate() {
        return advertisementRepository.getMaxPublishDate();
    }

}
