package com.hashedin.MetaDataExtraction.repository;

import com.hashedin.MetaDataExtraction.dto.Asset;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface ElementsRepository extends JpaRepository<Asset, Integer> {

}
