package com.ddkolesnik.siteparser.repository;

import com.ddkolesnik.siteparser.model.Advertisement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * @author Alexandr Stegnin
 */

@Repository
public interface AdvertisementRepository extends JpaRepository<Advertisement, Long> {

    @Query("DELETE FROM Advertisement adv WHERE adv.creationTime < :currrentDate")
    void deleteOld(@Param("currentDate") LocalDateTime currentDate);

}
