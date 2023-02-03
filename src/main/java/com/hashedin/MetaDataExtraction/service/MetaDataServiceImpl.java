package com.hashedin.MetaDataExtraction.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hashedin.MetaDataExtraction.config.BasicConfigProperties;
import com.hashedin.MetaDataExtraction.dto.*;
import com.hashedin.MetaDataExtraction.repository.ElementsRepository;
import com.hashedin.MetaDataExtraction.utils.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

@Service
@Slf4j
public class MetaDataServiceImpl {

    private final ElementsRepository elementsRepository;
    private final BasicConfigProperties basicConfigProperties;
    private final RestTemplate restTemplate;
    private final BearerTokenServiceImpl bearerTokenService;
    private static final Map<String, String> map = new LinkedHashMap<>();
    private static List<String> assetIds= new ArrayList<>();
    private static String key=null;

    @Autowired
    public MetaDataServiceImpl(ElementsRepository elementsRepository,
                               BasicConfigProperties basicConfigProperties, RestTemplate restTemplate,
                               BearerTokenServiceImpl bearerTokenService) {
        this.elementsRepository = elementsRepository;
        this.basicConfigProperties = basicConfigProperties;
        this.restTemplate = restTemplate;
        this.bearerTokenService = bearerTokenService;
    }

    public ResponseEntity<ElementResponse> getDownloadableUrl(String elementId){
        DateUtils dateUtils= new DateUtils();
        String url = basicConfigProperties.getGetUrl() + elementId+dateUtils.getExpiryDate()
                + basicConfigProperties.getElemProp();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.setBearerAuth(basicConfigProperties.getBearerToken());
        HttpEntity<String> entity = new HttpEntity<>(httpHeaders);
        ResponseEntity<ElementResponse> response=null;
        try {
            response = restTemplate.exchange(url, HttpMethod.GET,
                    entity, ElementResponse.class);
            log.info("Element Details Fetched ");
            basicConfigProperties.setAssetId(response.getBody().getAsset().getId());
        }catch(HttpStatusCodeException h){
            log.info("Error Status Code :"+h.getStatusCode());
            if(h.getStatusCode()==HttpStatus.UNAUTHORIZED){
                log.info("Bearer Token Expired");
                bearerTokenService.setBearerToken();
                httpHeaders.setBearerAuth(basicConfigProperties.getBearerToken());
                response = restTemplate.exchange(url, HttpMethod.GET,
                        entity, ElementResponse.class);
                basicConfigProperties.setAssetId(response.getBody().getAsset().getId());
            }
            if(h.getStatusCode()==HttpStatus.NOT_FOUND){
                log.warn("Element ID is Inavlid");
            }
        }
        return response;
    }


    public List<MetaDataFields> fetchMetaDataFields(ResponseEntity<ElementResponse> response) {
        Iterator<String> it=null;
        List<MetaDataFields> li = new ArrayList<MetaDataFields>();
        try {
            URL url = new URL(response.getBody().getDownloadUrl());
            URLConnection connection = url.openConnection();
            log.info("Connection opened to the URL");
            BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
            log.info("InputStream Obtained");
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLStreamReader reader = factory.createXMLStreamReader(in);
            printNote(reader);
            it=map.keySet().iterator();

            while(it.hasNext()){
                String ItemKey=it.next();
                String value=map.get(ItemKey);
                if(!value.contains(";")){
                    li.add(new MetaDataFields(ItemKey, value, false));
                }
            }
            map.clear();

        } catch (Exception e) {
            log.warn("Error Message: " + e.getMessage());
            log.warn("Error Cause: " + e.getCause());
        }
        return li;
    }

    private void printNote(XMLStreamReader reader) throws XMLStreamException {

        while (reader.hasNext()) {
            int event = reader.next();
            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    key=reader.getLocalName();
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    break;
                case XMLStreamConstants.CHARACTERS:
                    if(!reader.getText().matches("^[\\s]{1,}$")) {
                        if(map.containsKey(key)) {
                            map.replace(key, map.get(key)+"; "+reader.getText());
                        }
                        else {
                            map.put(key, reader.getText());
                        }
                    }
                    break;
                default:
            }
        }
        reader.close();
    }

    public Object addMetaData(List<MetaDataFields> li) {
        ResponseEntity<?> response=null;
        try {
            ObjectMapper mapper = new ObjectMapper();
            String jsonFormat = mapper.writeValueAsString(new MetaDataFormat(li));
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            httpHeaders.setBearerAuth(basicConfigProperties.getBearerToken());
            HttpEntity<String> entity = new HttpEntity<>(jsonFormat, httpHeaders);
            response = restTemplate.exchange(basicConfigProperties.getAddMetaDataApi() +
                    basicConfigProperties.getAssetId() + "/metadata", HttpMethod.POST, entity,
                    new ParameterizedTypeReference<>() {});
            log.info("MetaData Updated in corresponding Assets Custom MetaData Fields");
        } catch (JsonProcessingException j) {
            j.printStackTrace();
        }
        catch(HttpClientErrorException e){
            if(e.getStatusCode()==HttpStatus.CONFLICT){
                log.error("MetaData Already Exist");
            }
            if(e.getStatusCode()==HttpStatus.BAD_REQUEST){
                log.error("MetaData not Updated as PayLoad Format MISMATCH");
            }

        }
        li.clear();
        return response;
    }

    public boolean isXmlFile(ResponseEntity<ElementResponse> fileName){
        boolean status =false;
        try{
            String[] fileNameProps=fileName.getBody().getName().split("\\.");
            status= fileNameProps[fileNameProps.length-1].equalsIgnoreCase("xml");
        } catch(NullPointerException e){
            log.error("Error Message: " + "No Element Returned");
        }
        return status;
    }

    public List<String> getElementsId(){
        return elementsRepository.getElementIds();
    }

    public void changeStatusInDb(String elementId){
        elementsRepository.updateMetaDataStatus(elementId);
    }


    public void dbElements() {
        List<String> elementIds=getElementsId();
        Iterator<String> it = elementIds.iterator();
        while(it.hasNext()){
            String s=it.next();
            ResponseEntity<ElementResponse> response= getDownloadableUrl(s);
            if(isXmlFile(response)) {
                addMetaData(fetchMetaDataFields(response));
                changeStatusInDb(s);
            }
        }
    }
}