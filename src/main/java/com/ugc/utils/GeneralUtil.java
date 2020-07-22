package com.ugc.utils;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.ugc.exceptions.ApplicationException;

public class GeneralUtil {
    public static String getValueFromJson(String keyHeirarchy, String response) {
        String[] jsonKeys = keyHeirarchy.split("##");
        JSONParser parser = new JSONParser();
        JSONObject json = new JSONObject();
        try {
            json = (JSONObject) parser.parse(response);

            for (int i = 0; i <= jsonKeys.length - 1; i++) {
                if (jsonKeys.length - 1 == i)
                    return json.get(jsonKeys[i]).toString();
                json = (JSONObject) json.get(jsonKeys[i]);
            }
        } catch (ParseException e) {
            throw new ApplicationException(e, "could not parse response:{}", response);
        }
        return "";
    }
}
