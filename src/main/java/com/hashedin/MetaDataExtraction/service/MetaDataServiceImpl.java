package com.hashedin.MetaDataExtraction.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hashedin.MetaDataExtraction.config.BasicConfigProperties;
import com.hashedin.MetaDataExtraction.dto.ElementResponse;
import com.hashedin.MetaDataExtraction.dto.MetaData;
import com.hashedin.MetaDataExtraction.dto.MetaDataFields;
import com.hashedin.MetaDataExtraction.repository.ElementsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
@Service
public class MetaDataServiceImpl {

    private final ElementsRepository elementsRepository;
    private final BasicConfigProperties basicConfigProperties;
    private final RestTemplate restTemplate;
    private static final Map<String, String> map = new LinkedHashMap<String, String>();

    @Autowired
    public MetaDataServiceImpl(ElementsRepository elementsRepository,
                               BasicConfigProperties basicConfigProperties, RestTemplate restTemplate) {
        this.elementsRepository = elementsRepository;
        this.basicConfigProperties = basicConfigProperties;
        this.restTemplate = restTemplate;
    }

    public ResponseEntity<ElementResponse> getDownloadableUrl(String elementId){
        String url = basicConfigProperties.getGetUrl() + elementId
                + basicConfigProperties.getElemProp();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.setBearerAuth(basicConfigProperties.getBearerToken());
        HttpEntity<String> entity = new HttpEntity<>(httpHeaders);
        ResponseEntity<ElementResponse> response = restTemplate.exchange(url, HttpMethod.GET,
                entity, ElementResponse.class);
        basicConfigProperties.setAssetId(response.getBody().getAsset().getId());
        return response;
    }
    public List<MetaDataFields> fetchMetaDataFields(ResponseEntity<ElementResponse> response) {
        List<MetaDataFields> li = new ArrayList<MetaDataFields>();
            try {
                InputStream in = new URL(response.getBody().getDownloadUrl()).openStream();
                DocumentBuilder documentBuilder =
                        DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document doc = documentBuilder.parse(in);
                if (doc.hasChildNodes()) {
                    printNote(doc.getChildNodes(), "");
                } else {

                }

                for (Map.Entry<String, String> entry : map.entrySet()) {
                    if (!entry.getValue().contains(";")) {
                        li.add(new MetaDataFields(entry.getKey(), entry.getValue(), false));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        return li;
    }

    private void printNote(NodeList nodeList, String parent) {

        for (int count = 0; count < nodeList.getLength(); count++) {

            Node tempNode = nodeList.item(count);

            if (tempNode.hasChildNodes()) {

                printNote(tempNode.getChildNodes(),
                        tempNode.getNodeName());
            } else {
                if (tempNode.getNodeType() == Node.TEXT_NODE &&
                        !tempNode.getTextContent().toString().matches("^[\\s]{1,}$")) {
                    if (map.containsKey(parent)) {
                        map.replace(parent, map.get(parent) + "; " + tempNode.getTextContent());
                    } else {
                        map.put(parent, tempNode.getTextContent());
                    }

                }
            }

        }

    }

    public ResponseEntity<MetaData> addMetaData(List<MetaDataFields> li) {
        ResponseEntity<MetaData> response=null;
        try {
            ObjectMapper mapper = new ObjectMapper();
            String jsonFormat = mapper.writeValueAsString(new MetaData(li));
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            httpHeaders.setBearerAuth(basicConfigProperties.getBearerToken());
            HttpEntity<String> entity = new HttpEntity<>(jsonFormat, httpHeaders);
            response = restTemplate.exchange(basicConfigProperties.getAddMetaDataApi() +
                    basicConfigProperties.getAssetId() + "/metadata", HttpMethod.POST, entity, MetaData.class);
        } catch (JsonProcessingException j) {
            j.printStackTrace();
        }
        return response;
    }

    public boolean isXmlFile(String fileName){
        String[] fileNameProps=fileName.split("\\.");
        return fileNameProps[fileNameProps.length-1].equalsIgnoreCase("xml");
    }

}
