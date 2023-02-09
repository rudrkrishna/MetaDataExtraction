package com.hashedin.MetaDataExtraction.model;

import lombok.*;

import javax.persistence.*;

@Entity
@Table(name = "elements")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Element {
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;

    @Column(name = "element_name")
    private String elementName;

    @Column(name = "element_id")
    private String elementId;

    @Column(name = "asset_id")
    private String assetId;

    @Column(name = "added_to_custom_metadata")
    private Boolean addedToCustomMetadata = false;

    @Column(name = "workspace_id")
    private String workspaceId;
}
