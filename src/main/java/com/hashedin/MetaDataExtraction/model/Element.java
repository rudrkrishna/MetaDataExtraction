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

    @Column(name = "hash")
    private String hash;

    @Column(name = "path")
    private String path;

    @Column(name = "elementname")
    private String elementName;

    @Column(name = "format")
    private String format;

    @Column(name = "foldername")
    private String folderName;

    @Column(name = "folderid")
    private String folderId;

    @Column(name = "elementid")
    private String elementId;

    @Column(name = "assetid")
    private String assetId;

    @Column(name = "addedtoasset")
    private Boolean addedToAsset = false;

    @Column(name = "addedtocustommetadata")
    private Boolean addedToCustomMetadata = false;

    @Column(name = "workspaceid")
    private String workspaceId;
}
