package com.ugc;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.reflections.Reflections;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.github.os72.protocjar.Protoc;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorSet;
import com.google.protobuf.DescriptorProtos.MethodDescriptorProto;
import com.google.protobuf.DescriptorProtos.ServiceDescriptorProto;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.Descriptors.FileDescriptor;

import io.grpc.BindableService;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CommandExecuter implements CommandLineRunner {
	String protoFileRootDirectory = "D://workspace/kramphub-apis/src/main/proto";
	String protoFile = "/v1/postino.proto";
	String protoBufDir = "src/main/resources/protobuf-descriptors/";
	String pbFile = "descriptor.pb";

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

			// Channel channel = ManagedChannelBuilder.forAddress("",
			// 8080).useTransportSecurity().build();
			// channel.newCall(mdList.get(0).getDescriptor(). CallOptions.DEFAULT);

			Map<String, FileDescriptorProto> protofileToDescriptorProtoMap = set.getFileList().stream()
					.collect(Collectors.toMap(protoFile -> protoFile.getName(), protoFile -> protoFile));

			Map<String, FileDescriptor> protoFileToDescriptorMap = new HashMap<>();
			set.getFileList().forEach(
					fdp -> resolveFileDescriptor(fdp, protoFileToDescriptorMap, protofileToDescriptorProtoMap));

			System.out.println(protoFileToDescriptorMap);
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private FileDescriptorProto findFileDescriptorProto(String protoName, List<FileDescriptorProto> fileList) {
		return fileList.stream().filter(fileProto -> fileProto.getName().equals(protoName)).collect(Collectors.toList())
				.get(0);
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
