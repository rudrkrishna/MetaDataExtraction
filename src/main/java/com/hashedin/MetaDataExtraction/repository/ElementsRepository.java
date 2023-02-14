package com.hashedin.MetaDataExtraction.repository;

import com.hashedin.MetaDataExtraction.model.Element;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ElementsRepository extends JpaRepository<Element, Integer> {

    @Query(value = "select * from elements e where e.workspace_id = :workspaceId and e.element_id = :elementId and e.asset_id = :assetId", nativeQuery = true)
    Optional<Element> getElementDetails(@Param("workspaceId") String workspaceId, @Param("elementId") String elementId, @Param("assetId") String assetId);
}