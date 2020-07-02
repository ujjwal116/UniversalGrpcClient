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

import org.reflections.Reflections;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import com.github.os72.protocjar.Protoc;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorSet;
import com.google.protobuf.DescriptorProtos.MethodDescriptorProto;
import com.google.protobuf.DescriptorProtos.ServiceDescriptorProto;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.DynamicMessage.Builder;
import com.google.protobuf.TypeRegistry;
import com.google.protobuf.util.JsonFormat;

import io.grpc.BindableService;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.MethodDescriptor.MethodType;
import io.grpc.stub.ClientCalls;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CommandExecuter implements CommandLineRunner {
	String protoFileRootDirectory = "D://workspace/kramphub-apis/src/main/proto";
	String protoFile = "/v1/postino.proto";
	String protoBufDir = "src/main/resources/protobuf-descriptors/";
	String pbFile = "descriptor.pb";
	private String requestBody = "src/main/resources/requests/UpsertPreferencesByEventRequest.json";
	List<String> fullMethodNames = new ArrayList<>();

	public void buildClient() {
		Reflections reflections = new Reflections("com.kramphub");
		reflections.getSubTypesOf(BindableService.class);
	}

	public void run(String... args) {
		// if (Arrays.stream(args).anyMatch(s -> s.equalsIgnoreCase("listService")))
		listServices();
	}

	private void listServices() {
		try {
			List<String> files = List.of("/v1/postino.proto", "/v1/bouncer.proto");
			fullMethodNames.add("postino.OrganizationPreferencesService.UpsertPreferencesByEvent");

			Path descriptorPath = Path.of(new File(protoBufDir + pbFile).getAbsolutePath());
			List<String> protocArgs = new ArrayList<String>();
			protocArgs.add("--proto_path=" + protoFileRootDirectory);
			protocArgs.add("--descriptor_set_out=" + descriptorPath.toString());
			protocArgs.add("--include_imports");
			protocArgs.add(protoFileRootDirectory + files.get(0));
			protocArgs.add(protoFileRootDirectory + files.get(1));
			Protoc.runProtoc(protocArgs.toArray(new String[0]));

			FileDescriptorSet set = FileDescriptorSet.parseFrom(Files.readAllBytes(descriptorPath));
			List<ServiceDescriptorProto> sdList = new ArrayList<>();
			Map<String, List<String>> serviceToMethodListMap = new HashMap<>();
			List<MethodDescriptorProto> mdList = new ArrayList<>();
			set.getFileList().forEach(fileDescriptor -> fileDescriptor.getServiceList().forEach(sd -> {
				sdList.add(sd);
				buildServicesToMethodsMap(sd.getName(), sd.getMethodList(), serviceToMethodListMap);
				sd.getMethodList().forEach(mdList::add);
			}));

			log.info(serviceToMethodListMap.toString());

			Map<String, FileDescriptorProto> protofileToDescriptorProtoMap = set.getFileList().stream()
					.collect(Collectors.toMap(protoFile -> protoFile.getName(), protoFile -> protoFile));

			Map<String, FileDescriptor> protoFileToDescriptorMap = new HashMap<>();
			set.getFileList().forEach(
					fdp -> resolveFileDescriptor(fdp, protoFileToDescriptorMap, protofileToDescriptorProtoMap));

			Map<String, MethodDescriptor> fullNameToMethodDescriptorMap = fetchMethodDescriptorByMethodName(
					fullMethodNames.stream().collect(Collectors.toSet()), protoFileToDescriptorMap);

			TypeRegistry.Builder typeRegBuilder = TypeRegistry.newBuilder();
			protoFileToDescriptorMap.forEach((k, v) -> typeRegBuilder.add(v.getMessageTypes()));

			Builder messageBuilder = DynamicMessage
					.newBuilder(fullNameToMethodDescriptorMap.get(fullMethodNames.get(0)).getInputType());

			JsonFormat.parser().usingTypeRegistry(typeRegBuilder.build())
					.merge(new String(Files.readAllBytes(ResourceUtils.getFile(requestBody).toPath())), messageBuilder);
			messageBuilder.build();

			Channel channel = ManagedChannelBuilder.forAddress("localhost", 8080).useTransportSecurity().build();

			io.grpc.MethodDescriptor.Marshaller<DynamicMessage> requestMarshaller = new DynamicMessageMarshaller(
					fullNameToMethodDescriptorMap.get(fullMethodNames.get(0)).getInputType());

			io.grpc.MethodDescriptor.Marshaller<DynamicMessage> responseMarshaller = new DynamicMessageMarshaller(
					fullNameToMethodDescriptorMap.get(fullMethodNames.get(0)).getInputType());

			io.grpc.MethodDescriptor<DynamicMessage, DynamicMessage> gcpMethodDescriptor = io.grpc.MethodDescriptor
					.<DynamicMessage, DynamicMessage>newBuilder().setRequestMarshaller(requestMarshaller)
					.setResponseMarshaller(responseMarshaller).setFullMethodName(fullMethodNames.get(0))
					.setType(MethodType.UNARY).build();

			// CommandExecuter

			// (type, fullMethodName, requestMarshaller, responseMarshaller);
			ClientCalls.asyncUnaryCall(channel.newCall(gcpMethodDescriptor, CallOptions.DEFAULT),
					messageBuilder.build(), new ResponseObserver());

			/*
			 * ClientCall<DynamicMessage, DynamicMessage> caller =
			 * channel.newCall(gcpMethodDescriptor, CallOptions.DEFAULT).;
			 */
			System.out.println(protoFileToDescriptorMap);
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private Map<String, MethodDescriptor> fetchMethodDescriptorByMethodName(Set<String> setOfFullNames,
			Map<String, FileDescriptor> protoFileToDescriptorMap) {

		List<MethodDescriptor> mdList = new ArrayList<>();
		protoFileToDescriptorMap.forEach((k, v) -> v.getServices().forEach(sd -> {
			mdList.addAll(sd.getMethods());
		}));

		return mdList.stream().filter(md -> setOfFullNames.contains(md.getFullName()))
				.collect(Collectors.toMap(md -> md.getFullName(), md -> md));
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

			ImmutableList.Builder<FileDescriptor> prtoDependencyList = ImmutableList.builder();
			List<FileDescriptor> l = fdp.getDependencyList().stream().map(protofileToDescriptorMap::get)
					.collect(Collectors.toList());

			protofileToDescriptorMap.put(fdp.getName(),
					FileDescriptor.buildFrom(fdp, l.toArray(new FileDescriptor[fdp.getDependencyCount()])));
			return;
		} catch (DescriptorValidationException e) {
			log.error("Error while creating protoDescriptor for {} and exception is", fdp.getName(), e);
		}
	}

	private void buildServicesToMethodsMap(String service, List<MethodDescriptorProto> methodDescProtoList,
			Map<String, List<String>> serviceToMethodListMap) {
		List<String> methods = methodDescProtoList.stream().map(md -> md.getName()).collect(Collectors.toList());
		serviceToMethodListMap.put(service, methods);
	}
}
