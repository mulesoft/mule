/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.metadata;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.extension.ws.WscTestUtils.ECHO;
import static org.mule.extension.ws.WscTestUtils.ECHO_HEADERS;
import static org.mule.extension.ws.WscTestUtils.NO_PARAMS;

import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.NullType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.StringType;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;

import java.util.Collection;

import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Description;

public class HeadersMetadataTestCase extends AbstractMetadataTestCase {

  @Test
  @Description("Checks the input Soap Headers metadata for an operation with headers")
  public void getEchoInputHeaders() {
    MetadataResult<ComponentMetadataDescriptor> metadata = getMetadata(ECHO_HEADERS_FLOW, ECHO_HEADERS);
    MetadataType type = metadata.get().getInputMetadata().get().getParameterMetadata(HEADERS_PARAM).get().getType();
    ObjectType headers = toObjectType(type);

    Collection<ObjectFieldType> fields = headers.getFields();
    assertThat(fields, hasSize(2));
    fields.forEach(field -> {
      Collection<ObjectFieldType> headerFields = ((ObjectType) field.getValue()).getFields();
      assertThat(headerFields, hasSize(1));
      MetadataType headerField = headerFields.iterator().next().getValue();
      assertThat(headerField, is(instanceOf(StringType.class)));
    });
  }

  @Test
  @Description("Checks the input Soap Headers metadata for an operation without headers")
  public void getEchoNoHeaders() {
    MetadataResult<ComponentMetadataDescriptor> metadata = getMetadata(ECHO_FLOW, ECHO);
    MetadataType type = metadata.get().getInputMetadata().get().getParameterMetadata(HEADERS_PARAM).get().getType();
    assertThat(type, is(instanceOf(NullType.class)));
  }

  @Test
  @Description("Checks the input Soap Headers metadata for an operation without headers and without body params")
  public void getNoParamsNoHeaders() {
    MetadataResult<ComponentMetadataDescriptor> metadata = getMetadata(NO_PARAMS_FLOW, NO_PARAMS);
    MetadataType type = metadata.get().getInputMetadata().get().getParameterMetadata(HEADERS_PARAM).get().getType();
    assertThat(type, is(instanceOf(NullType.class)));
  }
}
