package com.ugc;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.github.os72.protocjar.Protoc;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorSet;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.DynamicMessage.Builder;
import com.google.protobuf.TypeRegistry;
import com.google.protobuf.util.JsonFormat;
import com.ugc.config.ArgumentConfig;
import com.ugc.exceptions.ApplicationException;
import com.ugc.utils.DynamicRequest;
import com.ugc.utils.RequestReaderUtil;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CommandExecuter implements CommandLineRunner {
    private RequestReaderUtil reqReaderUtil;

    public CommandExecuter(RequestReaderUtil reqReaderUtil) {
        this.reqReaderUtil = reqReaderUtil;
    }

    private static final String PROTOBUF_FILE = "descriptor.pb";

    private List<String> fullMethodNames = new ArrayList<>();

    public void run(String... args) {
        ArgumentConfig arguments = new ArgumentConfig(args);
        invokeGRPCServices(arguments);
        // testJS(arguments);
    }

    private void testJS(ArgumentConfig arguments) {
        ScriptEngine js = new ScriptEngineManager().getEngineByName("Nashorn");
        Path path = Path.of(new File(arguments.getRequestJsonDirectory() + "BouncerLogin.json").getAbsolutePath());
        try {
            String json = Files.readString(path);
            js.eval("var json1 = JSON.stringify(" + json + "); print(JSON.parse(json1).serviceCallOrder)");
            js.eval("print(json1)");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ScriptException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void invokeGRPCServices(ArgumentConfig arguments) {

        try {

            Path descriptorPath = Path.of(new File(arguments.getProtoBufDirectory() + PROTOBUF_FILE).getAbsolutePath());

            executeProtocandBuildProtoBuff(arguments, descriptorPath);

            FileDescriptorSet set = FileDescriptorSet
                    .parseFrom(Files.readAllBytes(descriptorPath));

            Map<String, FileDescriptorProto> protofileToDescriptorProtoMap = set.getFileList().stream()
                    .collect(Collectors.toMap(FileDescriptorProto::getName, protoFile -> protoFile));

            Map<String, FileDescriptor> protoFileToDescriptorMap = new HashMap<>();
            set.getFileList().forEach(
                    fdp -> resolveFileDescriptor(fdp, protoFileToDescriptorMap, protofileToDescriptorProtoMap));

            log.info("Processing proto files including depndencies: {}", protoFileToDescriptorMap.keySet());

            List<DynamicRequest> requests = reqReaderUtil.buildDynamicRequest(arguments.getRequestJsonDirectory());

            requests.sort((req1, req2) -> req1.getServiceCallOrder().compareTo(req2.getServiceCallOrder()));

            fullMethodNames = requests.stream().map(DynamicRequest::getFullMethodName).collect(Collectors.toList());

            Map<String, MethodDescriptor> fullNameToMethodDescriptorMap = buildMethodToMethodDescriptorMap(
                    protoFileToDescriptorMap);
            requests.stream().filter(rq -> fullNameToMethodDescriptorMap.containsKey(rq.getFullMethodName()))
                    .forEach(request -> prepareDynamicMessages(request, fullNameToMethodDescriptorMap));

            DynamicClient client = new DynamicClient(fullNameToMethodDescriptorMap);
            requests.stream().filter(rq -> !rq.isIgnoreThisRequest()).forEach(req -> {
                if (arguments.isPrintResponse()) {
                    log.info("Response for the method: {} is {}", req.getFullMethodName(), client.invokeService(req));
                }
            });

        } catch (Exception e) {
            throw new ApplicationException(e, " Error occured while bulding FilDecriptorSet");
        }
    }

    private void prepareDynamicMessages(DynamicRequest request,
            Map<String, MethodDescriptor> fullNameToMethodDescriptorMap) {
        TypeRegistry.Builder typeRegBuilder = TypeRegistry.newBuilder();
        Builder messageBuilder = DynamicMessage
                .newBuilder(fullNameToMethodDescriptorMap.get(request.getFullMethodName()).getInputType());

        try {
            JsonFormat.parser().usingTypeRegistry(typeRegBuilder.build())
                    .merge(request.getRequestJson(), messageBuilder);
            request.setMessage(messageBuilder.build());
        } catch (IOException e) {
            throw new ApplicationException(e,
                    "Error ocurred whille buildin Dynamic message for method:{}, Hence aborting the execition for all the requests",
                    request.getFullMethodName());
        }
    }

    private Map<String, MethodDescriptor> buildMethodToMethodDescriptorMap(
            Map<String, FileDescriptor> protoFileToDescriptorMap) {

        return fetchMethodDescriptorByMethodName(
                fullMethodNames.stream().collect(Collectors.toSet()), protoFileToDescriptorMap);
    }

    private void executeProtocandBuildProtoBuff(ArgumentConfig arguments, Path descriptorPath)
            throws IOException, InterruptedException {
        List<String> protocArgs = new ArrayList<>();
        protocArgs.add("--proto_path=" + arguments.getProtoRootDirectory());
        protocArgs.add("--descriptor_set_out=" + descriptorPath.toString());
        protocArgs.add("--include_imports");
        arguments.getProtoFiles().forEach(file -> protocArgs.add(arguments.getProtoRootDirectory() + file));
        Protoc.runProtoc(protocArgs.toArray(new String[0]));
    }

    private Map<String, MethodDescriptor> fetchMethodDescriptorByMethodName(Set<String> setOfFullNames,
            Map<String, FileDescriptor> protoFileToDescriptorMap) {

        List<MethodDescriptor> mdList = new ArrayList<>();
        protoFileToDescriptorMap.forEach((k, v) -> v.getServices().forEach(sd -> mdList.addAll(sd.getMethods())));

        return mdList.stream().filter(md -> setOfFullNames.contains(md.getFullName()))
                .collect(Collectors.toMap(MethodDescriptor::getFullName, md -> md));
    }

    private void resolveFileDescriptor(FileDescriptorProto fdp, Map<String, FileDescriptor> protofileToDescriptorMap,
            Map<String, FileDescriptorProto> protofileToDescriptorProtoMap) {
        try {
            if (fdp.getDependencyCount() == 0) {
                protofileToDescriptorMap.put(fdp.getName(), FileDescriptor.buildFrom(fdp, new FileDescriptor[0]));
                return;
            }
            if (protofileToDescriptorMap.containsKey(fdp.getName())) {
                return;
            }

            for (String fileName : fdp.getDependencyList()) {
                if (!protofileToDescriptorMap.containsKey(fileName)) {
                    resolveFileDescriptor(protofileToDescriptorProtoMap.get(fileName), protofileToDescriptorMap,
                            protofileToDescriptorProtoMap);
                }
            }

            List<FileDescriptor> l = fdp.getDependencyList().stream().map(protofileToDescriptorMap::get)
                    .collect(Collectors.toList());

            protofileToDescriptorMap.put(fdp.getName(),
                    FileDescriptor.buildFrom(fdp, l.toArray(new FileDescriptor[fdp.getDependencyCount()])));
        } catch (DescriptorValidationException e) {
            log.error("Error while creating protoDescriptor for {} and exception is", fdp.getName(), e);
        }
    }
}
