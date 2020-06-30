package com.ugc;

import java.util.Set;

import com.google.protobuf.AnyProto;
import com.google.protobuf.ApiProto;
import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DurationProto;
import com.google.protobuf.EmptyProto;
import com.google.protobuf.FieldMaskProto;
import com.google.protobuf.SourceContextProto;
import com.google.protobuf.StructProto;
import com.google.protobuf.TimestampProto;
import com.google.protobuf.TypeProto;
import com.google.protobuf.WrappersProto;

/**
 * Central place to store information about the protobuf well-known-types.
 */
public class WellKnownTypes {
	private static final Set<FileDescriptorProto> DESCRIPTORS = Set.of(AnyProto.getDescriptor().getFile().toProto(),
			ApiProto.getDescriptor().getFile().toProto(), DescriptorProto.getDescriptor().getFile().toProto(),
			DurationProto.getDescriptor().getFile().toProto(), EmptyProto.getDescriptor().getFile().toProto(),
			FieldMaskProto.getDescriptor().getFile().toProto(), SourceContextProto.getDescriptor().getFile().toProto(),
			StructProto.getDescriptor().getFile().toProto(), TimestampProto.getDescriptor().getFile().toProto(),
			TypeProto.getDescriptor().getFile().toProto(), WrappersProto.getDescriptor().getFile().toProto());

	private static final Set<String> FILES = Set.of("any.proto", "api.proto", "descriptor.proto", "duration.proto",
			"empty.proto", "field_mask.proto", "source_context.proto", "struct.proto", "timestamp.proto", "type.proto",
			"wrappers.proto");

	public static Set<FileDescriptorProto> descriptors() {
		return DESCRIPTORS;
	}

	public static Set<String> fileNames() {
		return FILES;
	}
}
