package com.ugc.utils;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.stereotype.Component;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.ugc.exceptions.ApplicationException;
import com.ugc.utils.DynamicRequest.DynamicRequestBuilder;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RequestReaderUtil {
    public List<DynamicRequest> buildDynamicRequest(String requestLocation) {
        List<DynamicRequest> requests = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(Paths.get(requestLocation))) {
            requests = paths
                    .filter(Files::isRegularFile)
                    .map(this::validateAndbuildDynamicRequest).collect(Collectors.toList());
        } catch (Exception e) {
            throw new ApplicationException(e, "Error occured while reading file from location: {}", requestLocation);
        }
        return requests;
    }

    private DynamicRequest validateAndbuildDynamicRequest(Path path) {
        JSONParser jsonParser = new JSONParser();
        DynamicRequestBuilder builder = DynamicRequest.builder();
        FileReader fr;
        try {
            fr = new FileReader(path.toString());
            JSONObject request = (JSONObject) jsonParser.parse(fr);

            validatRequest(path, request);
            if (!((JSONObject) request.get(UGCConstants.ENDPOINT_KEY)).containsKey(UGCConstants.SECURED_KEY)) {
                builder.isSecured(false);
            } else {
                builder.isSecured(Boolean.valueOf(((JSONObject) request.get(UGCConstants.ENDPOINT_KEY))
                        .get(UGCConstants.SECURED_KEY).toString()));
            }
            if (!request.containsKey(UGCConstants.CALL_ORDER_KEY)) {
                builder.serviceCallOrder(10000);
            } else {
                builder.serviceCallOrder(Integer.valueOf(request.get(UGCConstants.CALL_ORDER_KEY).toString()));
            }
            if (request.containsKey(UGCConstants.CROSS_REQUEST_KEY_TO_METADATA_KEY)) {
                builder.crossRequestKeyToMetadataKey(
                        convertJsonToMap(request.get(UGCConstants.CROSS_REQUEST_KEY_TO_METADATA_KEY).toString()));
            }
            if (request.containsKey(UGCConstants.FIELD_TO_CROSS_REQUEST_KEY)) {
                builder.responseFieldToCrossRequestKey(
                        convertJsonToMap(request.get(UGCConstants.FIELD_TO_CROSS_REQUEST_KEY).toString()));
            }
            if (request.containsKey(UGCConstants.IGNORE_THIS_REQUEST_KEY)) {
                builder.ignoreThisRequest(
                        Boolean.valueOf(request.get(UGCConstants.IGNORE_THIS_REQUEST_KEY).toString()));
            } else {
                builder.ignoreThisRequest(false);
            }

            return builder.host(((JSONObject) request.get(UGCConstants.ENDPOINT_KEY))
                    .get(UGCConstants.HOST_KEY).toString())
                    .port(Integer.valueOf(((JSONObject) request.get(UGCConstants.ENDPOINT_KEY))
                            .get(UGCConstants.PORT_KEY).toString()))
                    .metadata(convertJsonToMap(request.get(UGCConstants.METADATA_KEY).toString()))
                    .fullMethodName(request.get(UGCConstants.METHOD_NAME_KEY).toString())
                    .requestJson(request.get(UGCConstants.REQUEST_KEY).toString())
                    .name(path.getFileName().toString())
                    .build();
        } catch (Exception e) {
            throw new ApplicationException(e,
                    "Erroro occured wile processing file {}" + path.toString());
        }

    }

    private Map<String, String> convertJsonToMap(String jsonString)
            throws IOException {

        return new Gson()
                .fromJson(jsonString, new TypeToken<HashMap<String, String>>() {
                }.getType());
    }

    private void validatRequest(Path path, JSONObject request) {
        String fileNoutFoundMessage = "The key {} not found in file: {}";
        if (!request.containsKey(UGCConstants.PROTO_PATH_KEY)) {
            throw new ApplicationException(fileNoutFoundMessage,
                    UGCConstants.PROTO_PATH_KEY, path.toString());
        }
        if (!request.containsKey(UGCConstants.ENDPOINT_KEY)
                || !((JSONObject) request.get(UGCConstants.ENDPOINT_KEY))
                        .containsKey(UGCConstants.PORT_KEY)
                || !((JSONObject) request.get(UGCConstants.ENDPOINT_KEY))
                        .containsKey(UGCConstants.HOST_KEY)) {
            throw new ApplicationException("The key {} or {} or {} not found in file: {}",
                    UGCConstants.ENDPOINT_KEY, UGCConstants.PORT_KEY, UGCConstants.HOST_KEY,
                    path.toString());
        }
        if (!request.containsKey(UGCConstants.REQUEST_KEY)) {
            throw new ApplicationException(fileNoutFoundMessage,
                    UGCConstants.REQUEST_KEY, path.toString());
        }
        if (!request.containsKey(UGCConstants.METHOD_NAME_KEY)) {
            throw new ApplicationException(fileNoutFoundMessage,
                    UGCConstants.METHOD_NAME_KEY, path.toString());
        }
        if (!request.containsKey(UGCConstants.METADATA_KEY)) {
            throw new ApplicationException(fileNoutFoundMessage,
                    UGCConstants.METADATA_KEY, path.toString());
        }
    }
}
