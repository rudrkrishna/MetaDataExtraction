package com.hashedin.MetaDataExtraction.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hashedin.MetaDataExtraction.config.BasicConfigProperties;
import com.hashedin.MetaDataExtraction.dto.ElementResponse;
import com.hashedin.MetaDataExtraction.dto.Items;
import com.hashedin.MetaDataExtraction.dto.MetaDataFields;
import com.hashedin.MetaDataExtraction.dto.MetaDataFormat;
import com.hashedin.MetaDataExtraction.model.Element;
import com.hashedin.MetaDataExtraction.repository.ElementsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.BufferedInputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.stream.Collectors;

import static com.hashedin.MetaDataExtraction.utils.Constants.XML;

@Service
@Slf4j
public class MetaDataServiceImpl {

    private final BasicConfigProperties basicConfigProperties;
    private final RestTemplate restTemplate;
    private final ElementsRepository elementsRepository;


    @Autowired
    public MetaDataServiceImpl(BasicConfigProperties basicConfigProperties, RestTemplate restTemplate, ElementsRepository elementsRepository) {
        this.basicConfigProperties = basicConfigProperties;
        this.restTemplate = restTemplate;
        this.elementsRepository = elementsRepository;
    }

    public void fetchMetaDataFields(ElementResponse elementDetails, String workSpaceId, Map<String, Items> assetDetailsMap) {
        List<MetaDataFields> metaDataFieldList;
        String elementId = elementDetails.getId();
        boolean metaDataAddStatus;
        try {
            Optional<Element> dbElementDetails = elementsRepository.getElementDetails(workSpaceId, elementId, elementDetails.getAsset().getId());

            if (dbElementDetails.isEmpty()) {
                // translating elements which are present only in workspace
                metaDataFieldList = fetchAndTranslateElement(elementDetails.getDownloadUrl(), elementId, workSpaceId);
                if(!Objects.isNull(metaDataFieldList)) {
                    Element newElement = new Element();
                    newElement.setAddedToCustomMetadata(false);
                    newElement.setAssetId(elementDetails.getAsset().getId());
                    newElement.setElementId(elementId);
                    newElement.setElementName(elementDetails.getName());
                    newElement.setWorkspaceId(workSpaceId);
                    Element savedElement = elementsRepository.save(newElement);

                    metaDataAddStatus = addMetaData(metaDataFieldList, elementDetails.getAsset().getId(), assetDetailsMap);
                    if (metaDataAddStatus) {
                        savedElement.setAddedToCustomMetadata(true);
                        elementsRepository.save(savedElement);
                    }
                }
            } else if (!dbElementDetails.get().getAddedToCustomMetadata()) {
                // translating elements which are migrated to workspace from s3
                metaDataFieldList = fetchAndTranslateElement(elementDetails.getDownloadUrl(), elementId, workSpaceId);
                if(!Objects.isNull(metaDataFieldList)) {
                    metaDataAddStatus = addMetaData(metaDataFieldList, elementDetails.getAsset().getId(), assetDetailsMap);
                    if (metaDataAddStatus) {
                        dbElementDetails.get().setAddedToCustomMetadata(true);
                        elementsRepository.save(dbElementDetails.get());
                    }
                }
            } else {
                log.info("ElementId : {} present in workspaceId : {} is already uploaded", elementId, workSpaceId);
            }
        } catch (Exception e) {
            log.error("Something went wrong while fetching element details for elementId : {} present in workspaceId : {}, errorMessage : {}",
                    elementId, workSpaceId, e.getMessage());
        }
    }

    private List<MetaDataFields> fetchAndTranslateElement(String elementDownloadUrl, String elementId, String workSpaceId) {
        try {
            log.info("Initiating download and translation for element with elementId : {} present in workspaceId : {}", elementId, workSpaceId);
            Map<String, String> map;
            List<MetaDataFields> metaDataFieldList = new ArrayList<>();
            Iterator<String> it;
            URL url = new URL(elementDownloadUrl);
            URLConnection connection = url.openConnection();
            BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLStreamReader reader = factory.createXMLStreamReader(in);
            map = printNote(reader);
            it = map.keySet().iterator();
            while (it.hasNext()) {
                String ItemKey = it.next();
                String value = map.get(ItemKey);
                if (!(value == null)) {
                    metaDataFieldList.add(new MetaDataFields(ItemKey, value, false));
                }
            }
            log.info("Completed download and translation for element with elementId : {} present in workspaceId : {}", elementId, workSpaceId);
            return metaDataFieldList;
        } catch (Exception e) {
            log.error("Something went wrong while downloading and translating or the element : {} was deleted, errorMessage : {}", elementId, e.getMessage());
            return null;
        }
    }

    private Map<String, String> printNote(XMLStreamReader reader) throws XMLStreamException {
        Map<String, String> map = new LinkedHashMap<>();
        String key = null;
        while (reader.hasNext()) {
            int event = reader.next();
            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    key = reader.getLocalName();
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    break;
                case XMLStreamConstants.CHARACTERS:
                    if (!reader.getText().matches("^[\\s]{1,}$")) {
                        if (map.containsKey(key)) {
                            map.replace(key, null);
                        } else {
                            map.put(key, reader.getText());
                        }
                    }
                    break;
                default:
            }
        }
        reader.close();
        return map;
    }

    public boolean addMetaData(List<MetaDataFields> metaDataFieldList, String assetId, Map<String, Items> assetDetailsMap) {
        try {
            return uploadMetaData(metaDataFieldList, assetId);
        } catch (JsonProcessingException e) {
            log.error("Something went wrong while parsing json, errorMessage : {}", e.getMessage());
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                log.error("MetaData Already Exist for assetId : {}, errorMessage : {}", assetId, e.getMessage());
                return mergeExistingMetaDataAndRetryUpload(metaDataFieldList, assetId, assetDetailsMap.get(assetId).getMetadata());
            }
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                log.error("MetaData not updated as payLoad format mismatch for assetId : {}, errorMessage : {}", assetId, e.getMessage());
            }
        } catch (Exception e) {
            log.error("Something went wrong while adding custom metadata fields to assetId : {}, errorMessage : {}", assetId, e.getMessage());
        }
        return false;
    }

    private boolean mergeExistingMetaDataAndRetryUpload(List<MetaDataFields> metaDataFieldList, String assetId, List<MetaDataFields> existingAssetMetaData) {

        Map<String, MetaDataFields> combineMetaDataMap = metaDataFieldList.stream().collect(Collectors.toMap(MetaDataFields::getName, metaData -> metaData));
        try {
            for(MetaDataFields metaData: existingAssetMetaData){
                if(combineMetaDataMap.containsKey(metaData.getName())){
                    deleteMetaData(assetId, metaData.getName());
                }
            }

            List<MetaDataFields> metaDataFields = new ArrayList<>(combineMetaDataMap.values());
            return uploadMetaData(metaDataFields, assetId);

        } catch (JsonProcessingException e) {
            log.error("Something went wrong while parsing json, errorMessage : {}", e.getMessage());
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                log.error("MetaData Already Exist for assetId : {}, errorMessage : {}", assetId, e.getMessage());
                return true;
            }
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                log.error("MetaData not updated as payLoad format mismatch for assetId : {}, errorMessage : {}", assetId, e.getMessage());
            }
        } catch (Exception e) {
            log.error("Something went wrong while adding custom metadata fields to assetId : {}, errorMessage : {}", assetId, e.getMessage());
        }
        return false;
    }

    private boolean uploadMetaData(List<MetaDataFields> metaDataFieldList, String assetId) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        String jsonFormat = mapper.writeValueAsString(new MetaDataFormat(metaDataFieldList));
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.setBearerAuth(basicConfigProperties.getBearerToken());
        HttpEntity<String> entity = new HttpEntity<>(jsonFormat, httpHeaders);
        restTemplate.exchange(basicConfigProperties.getAddMetaDataApi() +
                        assetId + "/metadata", HttpMethod.POST, entity,
                new ParameterizedTypeReference<>() {
                });
        log.info("MetaData updated in corresponding asset custom metaData fields where assetId : {}", assetId);
        return true;
    }

    private void deleteMetaData(String assetId, String fieldName) {
        log.info("Deleting MetaData fieldName : {} for the corresponding assetId : {}", fieldName, assetId);
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.setBearerAuth(basicConfigProperties.getBearerToken());
        HttpEntity<String> entity = new HttpEntity<>(httpHeaders);
        restTemplate.exchange(basicConfigProperties.getDeleteMetaData() +
                        assetId + "/metadata/" + fieldName , HttpMethod.DELETE, entity,
                new ParameterizedTypeReference<>() {
                });
        log.info("MetaData fieldName : {} deleted for the corresponding assetId : {}", fieldName, assetId);
    }

    public boolean isXmlFile(String fileName) {
        try {
            String[] fileNameProps = fileName.split("\\.");
            return fileNameProps[fileNameProps.length - 1].equalsIgnoreCase(XML);
        } catch (Exception e) {
            log.error("Something went wrong while checking whether the file is xml, errorMessage : {}", e.getMessage());
            return false;
        }
    }

}