package com.ddkolesnik.siteparser.repository;

import com.ddkolesnik.siteparser.model.Advertisement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @author Alexandr Stegnin
 */

@Repository
public interface AdvertisementRepository extends JpaRepository<Advertisement, Long> {

    @Query("SELECT MAX(adv.publishDate) FROM Advertisement adv")
    LocalDate getMaxPublishDate();

    @Modifying
    @Query("UPDATE Advertisement adv SET adv.actual = FALSE WHERE adv.creationTime < :currentDate")
    void setNotActual(@Param("currentDate") LocalDateTime currentDate);

    void deleteByCreationTimeBefore(LocalDateTime currentDate);

}
