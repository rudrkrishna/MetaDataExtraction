
package com.hashedin.MetaDataExtraction.repository;

import com.hashedin.MetaDataExtraction.dto.Asset;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface ElementsRepository extends JpaRepository<Asset, Integer> {

    @Query(value = "select elementId from elements e where e.addedToAsset=1 and e.addedToCustomMetaData is NULL and e.format='xml';", nativeQuery = true)
    List<String> getElementIds();

    @Modifying
    @Transactional
    @Query(value="update elements set addedToCustomMetaData = 1 where elementId=:elementId", nativeQuery = true)
    void updateMetaDataStatus(String elementId);

}