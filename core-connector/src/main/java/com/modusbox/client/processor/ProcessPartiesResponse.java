package com.modusbox.client.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.json.JSONObject;
import org.json.XML;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;

public class ProcessPartiesResponse implements Processor {

    public void process(Exchange exchange) throws Exception {
        String response = exchange.getIn().getBody(String.class);
        JSONObject json = XML.toJSONObject(response);

        JSONObject partiesResponse = new JSONObject();
        Field changeMap = partiesResponse.getClass().getDeclaredField("map");
        changeMap.setAccessible(true);
        changeMap.set(partiesResponse, new LinkedHashMap<>());
        changeMap.setAccessible(false);

        partiesResponse.put("type", "BUSINESS");
        partiesResponse.put("idType", exchange.getIn().getHeader("idType"));
        partiesResponse.put("idValue", exchange.getIn().getHeader("idValue"));
        partiesResponse.put("firstName", ((JSONObject)json.get("COMMAND")).get("FNAME"));
        partiesResponse.put("lastName", ((JSONObject)json.get("COMMAND")).get("LNAME"));
        partiesResponse.put("displayName", ((JSONObject)json.get("COMMAND")).get("NAME"));
        //partiesResponse.put("dateOfBirth", ((JSONObject)json.get("COMMAND")).get("DOB"));

        exchange.getIn().setBody(partiesResponse.toString());

    }
}
