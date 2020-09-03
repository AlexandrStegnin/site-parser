package com.ddkolesnik.siteparser.repository;

import com.ddkolesnik.siteparser.model.Advertisement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author Alexandr Stegnin
 */

@Repository
public interface AdvertisementRepository extends JpaRepository<Advertisement, Long> {
}
