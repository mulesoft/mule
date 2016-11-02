/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.extension.file.common;

import static com.google.common.collect.ImmutableList.copyOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import org.mule.metadata.api.model.AnyType;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.java.api.utils.JavaTypeUtils;
import org.mule.runtime.api.metadata.MetadataService;
import org.mule.runtime.api.metadata.ProcessorId;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.TypeMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;

import java.util.List;

public class FileMetadataResolverTestCommon {

  public void testTreeNodeType(MetadataService service, Class type) {
    MetadataResult<ComponentMetadataDescriptor> list = service.getMetadata(new ProcessorId("list", "0"));
    assertThat(list.isSuccess(), is(true));
    TypeMetadataDescriptor payload = list.get().getOutputMetadata().get().getPayloadMetadata().get();
    List<ObjectFieldType> fields = copyOf(((ObjectType) payload.getType()).getFields());

    assertThat(fields, hasSize(3));
    assertAttributesMetadata(fields.get(0), type);
    assertThat(fields.get(1).getValue(), instanceOf(AnyType.class));
    assertThat(fields.get(2).getValue(), instanceOf(ArrayType.class));
  }

  public void testReadAttributesMetadata(MetadataService service, Class type) {
    MetadataResult<ComponentMetadataDescriptor> read = service.getMetadata(new ProcessorId("read", "0"));

    assertThat(read.isSuccess(), is(true));
    TypeMetadataDescriptor attributes = read.get().getOutputMetadata().get().getAttributesMetadata().get();
    assertAttributesMetadata(attributes.getType(), type);
  }

  private void assertAttributesMetadata(MetadataType metadataType, Class attributesType) {
    Class type = JavaTypeUtils.getType(metadataType);
    assertThat(type, equalTo(attributesType));
  }
}
