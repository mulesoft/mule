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
import static org.hamcrest.Matchers.isIn;
import static org.junit.Assert.assertThat;
import static org.mule.extension.ws.WscTestUtils.ECHO;
import static org.mule.extension.ws.WscTestUtils.ECHO_ACCOUNT;
import static org.mule.extension.ws.WscTestUtils.ECHO_HEADERS;
import org.mule.metadata.api.model.DictionaryType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.NullType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.StringType;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;

import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;

import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Description;

public class OutputMetadataTestCase extends AbstractMetadataTestCase {

  @Test
  @Description("Checks the Output Body Metadata for an operation that returns a simple string")
  public void getEchoOutputBodyMetadata() {
    MetadataResult<ComponentMetadataDescriptor> result = getMetadata(ECHO_FLOW, ECHO);
    MetadataType type = result.get().getOutputMetadata().get().getPayloadMetadata().get().getType();
    Collection<ObjectFieldType> resultFields = toObjectType(type).getFields();
    assertThat(resultFields, hasSize(1));
    ObjectType echoType = toObjectType(resultFields.iterator().next().getValue());
    Collection<ObjectFieldType> echoFields = echoType.getFields();
    assertThat(echoFields, hasSize(1));
    ObjectFieldType textField = echoFields.iterator().next();
    assertThat(textField.getKey().getName().getLocalPart(), is("text"));
    assertThat(textField.getValue(), is(instanceOf(StringType.class)));
  }

  @Test
  @Description("Checks the Output Body Metadata for an operation that returns a complex element")
  public void getEchoAccountOutputBodyMetadata() {
    MetadataResult<ComponentMetadataDescriptor> result = getMetadata(ECHO_ACCOUNT_FLOW, ECHO_ACCOUNT);
    MetadataType type = result.get().getOutputMetadata().get().getPayloadMetadata().get().getType();
    Collection<ObjectFieldType> operationFields = toObjectType(type).getFields();
    assertThat(operationFields, hasSize(1));
    ObjectType echoType = toObjectType(operationFields.iterator().next().getValue());
    Collection<ObjectFieldType> echoFields = echoType.getFields();
    assertThat(echoFields, hasSize(1));
    ObjectFieldType accountField = echoFields.iterator().next();
    assertThat(accountField.getKey().getName().getLocalPart(), is("account"));
    ObjectType objectType = toObjectType(accountField.getValue());
    assertThat(objectType.getFields(), hasSize(4));
  }

  @Test
  @Description("Checks the Output Attributes Metadata for an operation without output soap headers")
  public void getEchoOutputAttributesMetadata() {
    MetadataResult<ComponentMetadataDescriptor> result = getMetadata(ECHO_FLOW, ECHO);
    MetadataType type = result.get().getOutputMetadata().get().getAttributesMetadata().get().getType();
    Collection<ObjectFieldType> attributeFields = toObjectType(type).getFields();
    assertThat(attributeFields, hasSize(2));

    Iterator<ObjectFieldType> iterator = attributeFields.iterator();
    MetadataType soapHeaders = iterator.next().getValue();
    MetadataType protocolHeaders = iterator.next().getValue();
    assertThat(soapHeaders, is(instanceOf(NullType.class)));
    assertThat(protocolHeaders, is(instanceOf(DictionaryType.class)));
  }

  @Test
  @Description("Checks the Output Attributes Metadata for an operation with output soap headers")
  public void getEchoWithHeadersOutputAttributesMetadata() {
    MetadataResult<ComponentMetadataDescriptor> result = getMetadata(ECHO_HEADERS_FLOW, ECHO_HEADERS);
    MetadataType type = result.get().getOutputMetadata().get().getAttributesMetadata().get().getType();
    Collection<ObjectFieldType> attributesFields = toObjectType(type).getFields();
    assertThat(attributesFields, hasSize(2));
    Optional<ObjectFieldType> soapHeaders = attributesFields.stream().filter(e -> e.getValue() instanceof ObjectType).findFirst();
    assertThat(soapHeaders.isPresent(), is(true));
    assertThat(soapHeaders.get().getKey().getName().getLocalPart(), is("soapHeaders"));
    Collection<ObjectFieldType> soapHeadersElements = toObjectType(soapHeaders.get().getValue()).getFields();
    assertThat(soapHeadersElements, hasSize(2));
    soapHeadersElements.forEach(e -> {
      assertThat(e.getKey().getName().getLocalPart(), isIn(new String[] {"headerOut", "headerInOut"}));
      MetadataType value = toObjectType(e.getValue()).getFields().iterator().next().getValue();
      assertThat(value, is(instanceOf(StringType.class)));
    });
  }
}
