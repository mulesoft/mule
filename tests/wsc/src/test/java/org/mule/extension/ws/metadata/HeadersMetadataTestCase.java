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
import static org.mule.extension.ws.WscTestUtils.ECHO_ACCOUNT;
import static org.mule.extension.ws.WscTestUtils.ECHO_HEADERS;
import static org.mule.services.soap.internal.metadata.SoapOutputTypeBuilder.BODY_FIELD;
import static org.mule.services.soap.internal.metadata.SoapOutputTypeBuilder.HEADERS_FIELD;
import static org.mule.test.allure.AllureConstants.WscFeature.WSC_EXTENSION;
import org.mule.metadata.api.annotation.TypeIdAnnotation;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.NullType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.StringType;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;

import java.util.Collection;
import java.util.List;

import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features(WSC_EXTENSION)
@Stories("Metadata")
public class HeadersMetadataTestCase extends AbstractMetadataTestCase {

  @Test
  @Description("Checks the input Soap Headers metadata for an operation with headers")
  public void getEchoInputHeaders() {
    ObjectType headers = toObjectType(getHeaders(ECHO_HEADERS_FLOW, ECHO_HEADERS));

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
    MetadataType type = getHeaders(ECHO_ACCOUNT_FLOW, ECHO_ACCOUNT);
    assertThat(type, is(instanceOf(NullType.class)));
  }

  @Test
  @Description("Checks the metadata for a Header that is defined in another message that is not the main operation message")
  public void getCommonHeaderDefinedInDifferentMessageMetadata() {
    ObjectType headers = toObjectType(getHeaders("sharedHeader", "Get_Employee"));
    assertThat(headers.getFields(), hasSize(1));
    ObjectFieldType header = headers.getFields().iterator().next();
    assertThat(header.getKey().getName().getLocalPart(), is("header"));
    assertThat(header.getValue().getAnnotation(TypeIdAnnotation.class).get().getValue(),
               is("{urn:com.workday/bsvc}Workday_Common_Header"));

  }


  private MetadataType getHeaders(String flow, String key) {
    MetadataResult<ComponentMetadataDescriptor<OperationModel>> result = getMetadata(flow, key);
    return getParameterType(result.get().getModel().getAllParameterModels(), HEADERS_FIELD);
  }
}
