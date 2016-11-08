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
import static org.mule.extension.ws.internal.metadata.BaseWscResolver.HEADERS_FIELD;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.NullType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.StringType;

import java.util.Collection;

import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Web Service Consumer")
@Stories("Metadata")
public class HeadersMetadataTestCase extends AbstractMetadataTestCase {

  @Test
  @Description("Checks the input Soap Headers metadata for an operation with headers")
  public void getEchoInputHeaders() {
    ObjectType message = getMessageBuilderType(ECHO_HEADERS_FLOW, ECHO_HEADERS);
    MetadataType type = getMessageBuilderFieldType(message, HEADERS_FIELD);
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
    ObjectType message = getMessageBuilderType(ECHO_ACCOUNT_FLOW, ECHO_ACCOUNT);
    MetadataType type = getMessageBuilderFieldType(message, HEADERS_FIELD);
    assertThat(type, is(instanceOf(NullType.class)));
  }
}
