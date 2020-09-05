package com.ddkolesnik.siteparser.service;

import com.ddkolesnik.siteparser.model.Advertisement;
import com.ddkolesnik.siteparser.repository.AdvertisementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public Advertisement create(Advertisement advertisement) {
        return advertisementRepository.save(advertisement);
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

}
