package com.ugc.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
@ToString
public class ArgumentConfig {

    static final String PROTO_ROOT_DIRECTORY = "protoRootDirectory";
    static final String PROTO_BUF_DIRECTORY = "protoBufDirectory";
    static final String PROTO_FILES_DIRECTORY = "protoFiles";
    static final String REQUEST_JSON_DIRECTORY = "requestJsonDirectory";
    private int i;

    private final boolean printResponse;
    private final String protoRootDirectory;
    private final String protoBufDirectory;
    private List<String> protoFiles;
    private final String requestJsonDirectory;

    public ArgumentConfig(String... args) {
        Map<String, List<String>> argMap = buildArgMap(args);
        validateMandatoryArguments(argMap);
        if (argMap.containsKey("printResponse")) {
            this.printResponse = true;
        } else {
            this.printResponse = false;
        }
        protoRootDirectory = argMap.get(PROTO_ROOT_DIRECTORY).get(0);
        protoBufDirectory = argMap.get(PROTO_BUF_DIRECTORY).get(0);
        protoFiles = argMap.get(PROTO_FILES_DIRECTORY);
        requestJsonDirectory = argMap.get(REQUEST_JSON_DIRECTORY).get(0);

    }

    private void validateMandatoryArguments(Map<String, List<String>> argMap) {
        if (!argMap.containsKey(PROTO_BUF_DIRECTORY) || argMap.get(PROTO_BUF_DIRECTORY).size() != 1) {
            log.error("Missing mandatory argument: {}", PROTO_BUF_DIRECTORY);
            throw new IllegalArgumentException("Argument protoBufDirectory is mendotory");
        }
        if (!argMap.containsKey(PROTO_ROOT_DIRECTORY) || argMap.get(PROTO_ROOT_DIRECTORY).size() != 1) {
            log.error("Missing mandatory argument:{}", PROTO_ROOT_DIRECTORY);
            throw new IllegalArgumentException("Argument protoBufDirectory is mendotory");
        }
        if (!argMap.containsKey(PROTO_FILES_DIRECTORY) || argMap.get(PROTO_FILES_DIRECTORY).isEmpty()) {
            log.error("Missing mandatory argument: {}", PROTO_FILES_DIRECTORY);
            throw new IllegalArgumentException("Argument protoFiles is mendotory");
        }
        if (!argMap.containsKey(REQUEST_JSON_DIRECTORY) || argMap.get(REQUEST_JSON_DIRECTORY).size() != 1) {
            log.error("Missing mandatory argument:{}", REQUEST_JSON_DIRECTORY);
            throw new IllegalArgumentException("Argument requestJsonDirectory is mendotory");
        }
    }

    private Map<String, List<String>> buildArgMap(String[] args) {

        Map<String, List<String>> argMap = new HashMap<>();
        while (i < args.length) {
            String command = args[i].substring(2);
            List<String> values = new ArrayList();
            i++;
            addValuesToList(args, values);

            argMap.put(command, values);
        }
        return argMap;

    }

    private void addValuesToList(String[] args, List<String> values) {
        if (args.length > i && !args[i].startsWith("--")) {
            values.add(args[i]);
            i++;
            if (args.length > i)
                addValuesToList(args, values);
        }
    }
}
