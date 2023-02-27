package com.hashedin.MetaDataExtraction.utils;

import com.hashedin.MetaDataExtraction.dto.MetaDataFields;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.BufferedInputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.hashedin.MetaDataExtraction.utils.Constants.XML;


@Service
@Slf4j
public class DownloadAndProcessingUtil {

    public List<MetaDataFields> fetchAndTranslateElement(String elementDownloadUrl, String elementId, String workSpaceId) {
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
