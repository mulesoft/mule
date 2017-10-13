/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.message;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.api.metadata.DataType.BOOLEAN;
import static org.mule.runtime.api.metadata.DataType.HTML_STRING;
import static org.mule.runtime.api.metadata.DataType.JSON_STRING;
import static org.mule.runtime.api.metadata.DataType.OBJECT;
import static org.mule.runtime.api.metadata.DataType.STRING;
import static org.mule.runtime.api.metadata.DataType.TEXT_STRING;
import static org.mule.runtime.api.metadata.DataType.fromObject;
import static org.mule.runtime.api.metadata.MediaType.ANY;
import static org.mule.runtime.api.metadata.MediaType.APPLICATION_JSON;
import static org.mule.runtime.api.metadata.MediaType.HTML;
import static org.mule.runtime.api.metadata.MediaType.TEXT;
import static org.mule.runtime.api.metadata.MediaType.XML;
import static org.mule.tck.junit4.matcher.DataTypeCompatibilityMatcher.assignableTo;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.internal.metadata.DefaultCollectionDataType;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.activation.DataHandler;

import org.junit.Test;

public class DefaultMuleMessageBuilderTestCase extends AbstractMuleTestCase {

  private static final String NEW_PAYLOAD = "new payload";
  private static final String EMPTY_JSON = "{}";
  private static final Object BASE_ATTRIBUTES = new BaseAttributes() {};
  private static final DataType BASE_ATTRIBUTES_DATATYPE = fromObject(BASE_ATTRIBUTES);
  private static final String PROPERTY_KEY = "propertyKey";
  private static final Serializable PROPERTY_VALUE = "propertyValue";
  private static final MediaType HTML_STRING_UTF8 = HTML.withCharset(UTF_8);

  @Test
  public void createNewAPIMessageViaMessageInterface() {
    org.mule.runtime.api.message.Message message;
    message = org.mule.runtime.api.message.Message.builder()
        .value(TEST_PAYLOAD)
        .mediaType(HTML_STRING_UTF8)
        .build();

    assertThat(message.getPayload().getValue(), is(TEST_PAYLOAD));
    assertThat(message.getPayload().getDataType().getType(), equalTo(String.class));
    assertThat(message.getPayload().getDataType().getMediaType(), is(HTML_STRING_UTF8));
    assertThat(message.getAttributes().getValue(), is(nullValue()));
    assertThat(message.getAttributes().getDataType().getMediaType(), is(OBJECT.getMediaType()));
  }

  @Test
  public void createAPIMessageViaMessageInterfaceFromCopy() {
    org.mule.runtime.api.message.Message message;
    message = org.mule.runtime.api.message.Message.builder().value(TEST_PAYLOAD).build();

    org.mule.runtime.api.message.Message messageCopy;
    messageCopy = org.mule.runtime.api.message.Message.builder(message).value(true).attributesValue(BASE_ATTRIBUTES).build();

    assertThat(messageCopy.getPayload().getValue(), is(true));
    assertThat(messageCopy.getPayload().getDataType(), is(BOOLEAN));
    assertThat(messageCopy.getAttributes().getValue(), is(BASE_ATTRIBUTES));
    assertThat(messageCopy.getAttributes().getDataType(), is(BASE_ATTRIBUTES_DATATYPE));
  }

  @Test
  public void createNewMessageViaMessageInterface() {
    Message message = Message.builder().value(TEST_PAYLOAD).build();

    assertThat(message.getPayload().getValue(), is(TEST_PAYLOAD));
    assertThat(message.getPayload().getDataType(), is(STRING));
    assertThat(message.getAttributes().getValue(), is(nullValue()));
  }

  @Test
  public void createNewMessageCollectionViaMessageInterface() {
    List<String> htmlStringList = new ArrayList<>();
    htmlStringList.add("HTML1");
    htmlStringList.add("HTML2");
    htmlStringList.add("HTML3");

    Message message =
        InternalMessage.builder().collectionValue(htmlStringList, String.class).itemMediaType(HTML)
            .build();

    assertThat(message.getPayload().getValue(), is(htmlStringList));
    assertThat(message.getPayload().getDataType().getType(), equalTo(ArrayList.class));
    assertThat(message.getPayload().getDataType().getMediaType(), is(ANY));
    assertThat(message.getPayload().getDataType(), instanceOf(DefaultCollectionDataType.class));
    assertThat(((DefaultCollectionDataType) message.getPayload().getDataType()).getItemDataType().getMediaType(), equalTo(HTML));
  }

  @Test
  public void createNewMessageCollectionViaMessageInterfaceCopy() {
    List<String> htmlStringList = new ArrayList<>();
    htmlStringList.add("HTML1");
    htmlStringList.add("HTML2");
    htmlStringList.add("HTML3");

    Message message =
        InternalMessage.builder().collectionValue(htmlStringList, String.class).itemMediaType(HTML)
            .build();

    Message copy = InternalMessage.builder(message).build();

    assertThat(copy.getPayload().getValue(), is(htmlStringList));
    assertThat(copy.getPayload().getDataType().getType(), equalTo(ArrayList.class));
    assertThat(copy.getPayload().getDataType().getMediaType(), is(ANY));
    assertThat(copy.getPayload().getDataType(), instanceOf(DefaultCollectionDataType.class));
    assertThat(((DefaultCollectionDataType) copy.getPayload().getDataType()).getItemDataType().getMediaType(), equalTo(HTML));
  }

  @Test
  public void createMessageViaMessageInterfaceFromCopy() {
    Message messageCopy = InternalMessage.builder(createTestMessage()).value(true).attributesValue(BASE_ATTRIBUTES).build();

    assertThat(messageCopy.getPayload().getValue(), is(true));
    assertThat(messageCopy.getPayload().getDataType(), is(assignableTo(BOOLEAN)));
    assertThat(messageCopy.getAttributes().getValue(), is(BASE_ATTRIBUTES));
    assertThat(messageCopy.getAttributes().getDataType(), is(BASE_ATTRIBUTES_DATATYPE));
  }

  @Test
  public void testOnlyPayload() {
    Message message = of(TEST_PAYLOAD);
    assertThat(message.getPayload().getValue(), is(TEST_PAYLOAD));
  }

  @Test
  public void wholePayload() {
    Message message = Message.builder().payload(new TypedValue<>(EMPTY_JSON, JSON_STRING)).build();

    assertThat(message.getPayload().getValue(), equalTo(EMPTY_JSON));
    assertThat(message.getPayload().getDataType().getType(), equalTo(String.class));
    assertThat(message.getPayload().getDataType().getMediaType(), is(APPLICATION_JSON));
  }

  @Test
  public void messageAttributes() {
    assertTestMessage(createTestMessage());
  }

  @Test
  public void wholeAttributes() {
    Message message = Message.builder().nullValue().attributes(new TypedValue<>(EMPTY_JSON, JSON_STRING)).build();

    assertThat(message.getAttributes().getValue(), equalTo(EMPTY_JSON));
    assertThat(message.getAttributes().getDataType().getType(), equalTo(String.class));
    assertThat(message.getAttributes().getDataType().getMediaType(), is(APPLICATION_JSON));
  }

  @Test
  public void messageAttributesCopy() {
    assertTestMessage(new DefaultMessageBuilder(createTestMessage()).build());
  }

  @Test
  public void inboundPropertyMap() {
    Map<String, Serializable> inboundProperties = singletonMap(PROPERTY_KEY, PROPERTY_VALUE);
    InternalMessage message = new DefaultMessageBuilder().value(TEST_PAYLOAD).inboundProperties(inboundProperties).build();

    assertThat(message.getInboundProperty(PROPERTY_KEY), equalTo(PROPERTY_VALUE));
    assertThat(message.getInboundPropertyDataType(PROPERTY_KEY), equalTo(STRING));
    assertThat(message.getInboundPropertyNames(), hasSize(1));
    assertThat(message.getInboundPropertyNames(), hasItem(PROPERTY_KEY));
  }

  @Test
  public void inboundPropertyMapCopy() {
    Map<String, Serializable> inboundProperties = singletonMap(PROPERTY_KEY, PROPERTY_VALUE);
    InternalMessage copy = new DefaultMessageBuilder(new DefaultMessageBuilder().value(TEST_PAYLOAD)
        .inboundProperties(inboundProperties).build()).build();

    assertThat(copy.getInboundProperty(PROPERTY_KEY), equalTo(PROPERTY_VALUE));
    assertThat(copy.getInboundPropertyDataType(PROPERTY_KEY), equalTo(STRING));
    assertThat(copy.getInboundPropertyNames(), hasSize(1));
    assertThat(copy.getInboundPropertyNames(), hasItem(PROPERTY_KEY));
  }

  @Test
  public void outboundPropertyMap() {
    Map<String, Serializable> outboundProperties = singletonMap(PROPERTY_KEY, PROPERTY_VALUE);
    InternalMessage message = new DefaultMessageBuilder().value(TEST_PAYLOAD).outboundProperties(outboundProperties).build();

    assertThat(message.getOutboundProperty(PROPERTY_KEY), equalTo(PROPERTY_VALUE));
    assertThat(message.getOutboundPropertyDataType(PROPERTY_KEY), equalTo(STRING));
    assertThat(message.getOutboundPropertyNames(), hasSize(1));
    assertThat(message.getOutboundPropertyNames(), hasItem(PROPERTY_KEY));
  }

  @Test
  public void outboundPropertyMapCopy() {
    Map<String, Serializable> outboundProperties = singletonMap(PROPERTY_KEY, PROPERTY_VALUE);
    InternalMessage copy = new DefaultMessageBuilder(new DefaultMessageBuilder().value(TEST_PAYLOAD)
        .outboundProperties(outboundProperties).build()).build();

    assertThat(copy.getOutboundProperty(PROPERTY_KEY), equalTo(PROPERTY_VALUE));
    assertThat(copy.getOutboundPropertyDataType(PROPERTY_KEY), equalTo(STRING));
    assertThat(copy.getOutboundPropertyNames(), hasSize(1));
    assertThat(copy.getOutboundPropertyNames(), hasItem(PROPERTY_KEY));
  }

  @Test
  public void inboundProperty() {
    InternalMessage message =
        new DefaultMessageBuilder().value(TEST_PAYLOAD).addInboundProperty(PROPERTY_KEY, PROPERTY_VALUE).build();

    assertThat(message.getInboundProperty(PROPERTY_KEY), equalTo(PROPERTY_VALUE));
    assertThat(message.getInboundPropertyDataType(PROPERTY_KEY), equalTo(STRING));
    assertThat(message.getInboundPropertyNames(), hasSize(1));
    assertThat(message.getInboundPropertyNames(), hasItem(PROPERTY_KEY));
  }

  @Test
  public void inboundPropertyDataType() {
    InternalMessage message =
        new DefaultMessageBuilder().value(TEST_PAYLOAD).addInboundProperty(PROPERTY_KEY, PROPERTY_VALUE, HTML_STRING).build();

    assertThat(message.getInboundProperty(PROPERTY_KEY), equalTo(PROPERTY_VALUE));
    assertThat(message.getInboundPropertyDataType(PROPERTY_KEY), equalTo(HTML_STRING));
    assertThat(message.getInboundPropertyNames(), hasSize(1));
    assertThat(message.getInboundPropertyNames(), hasItem(PROPERTY_KEY));
  }

  @Test
  public void outboundProperty() {
    InternalMessage message = new DefaultMessageBuilder().value(TEST_PAYLOAD).addOutboundProperty(PROPERTY_KEY, PROPERTY_VALUE)
        .build();

    assertThat(message.getOutboundProperty(PROPERTY_KEY), equalTo(PROPERTY_VALUE));
    assertThat(message.getOutboundPropertyDataType(PROPERTY_KEY), equalTo(STRING));
    assertThat(message.getOutboundPropertyNames(), hasSize(1));
    assertThat(message.getOutboundPropertyNames(), hasItem(PROPERTY_KEY));
  }

  @Test
  public void outboundPropertyDataType() {
    InternalMessage message = new DefaultMessageBuilder().value(TEST_PAYLOAD)
        .addOutboundProperty(PROPERTY_KEY, PROPERTY_VALUE, HTML_STRING).build();

    assertThat(message.getOutboundProperty(PROPERTY_KEY), equalTo(PROPERTY_VALUE));
    assertThat(message.getOutboundPropertyDataType(PROPERTY_KEY), equalTo(HTML_STRING));
    assertThat(message.getOutboundPropertyNames(), hasSize(1));
    assertThat(message.getOutboundPropertyNames(), hasItem(PROPERTY_KEY));
  }

  @Test
  public void nullPayload() {
    Message message = of(null);
    assertThat(message.getPayload().getDataType().getType(), equalTo(Object.class));
  }

  @Test
  public void mutateEntirePayload() {
    Message message = createTestMessage();
    Message copy = new DefaultMessageBuilder(message).payload(new TypedValue<>(EMPTY_JSON, JSON_STRING)).build();

    assertThat(copy.getPayload().getValue(), equalTo(EMPTY_JSON));
    assertThat(copy.getPayload().getDataType().getType(), equalTo(String.class));
    assertThat(copy.getPayload().getDataType().getMediaType(), is(APPLICATION_JSON));
  }

  @Test
  public void mutatePayloadSameTypeConserveTypeAndMimeType() {
    Message message = createTestMessage();
    Message copy = new DefaultMessageBuilder(message).value(NEW_PAYLOAD).build();

    assertThat(copy.getPayload().getValue(), equalTo(NEW_PAYLOAD));
    assertThat(copy.getPayload().getDataType().getType(), equalTo(String.class));
    assertThat(copy.getPayload().getDataType().getMediaType(), is(TEXT));
  }

  @Test
  public void mutatePayloadDifferentTypeUpdateTypeAndConserveMimeType() {
    Message copy = new DefaultMessageBuilder(createTestMessage()).value(1).build();

    assertThat(copy.getPayload().getValue(), equalTo(1));
    assertThat(copy.getPayload().getDataType().getType(), equalTo(Integer.class));
    assertThat(copy.getPayload().getDataType().getMediaType(), is(TEXT));
  }

  @Test
  public void mutatePayloadDifferentTypeWithMediaTypeUpdateTypeAndConserveMimeType() {
    Long payload = new Long(1);
    DataHandler dataHandler = new DataHandler(payload, XML.toString());
    Message copy = new DefaultMessageBuilder(createTestMessage()).value(dataHandler).build();

    assertThat(copy.getPayload().getValue(), is(dataHandler));
    assertThat(copy.getPayload().getDataType().getType(), equalTo(DataHandler.class));
    assertThat(copy.getPayload().getDataType().getMediaType(), is(XML));
  }

  @Test
  public void copyPreservesDataType() {
    Apple apple = new Apple();
    long appleSize = 111;
    Message message =
        new DefaultMessageBuilder().payload(new TypedValue(apple, fromObject(apple), Optional.of(appleSize))).build();
    Message copy = new DefaultMessageBuilder(message).build();

    assertThat(copy.getPayload(), is(message.getPayload()));
    assertThat(copy.getAttributes(), is(message.getAttributes()));
    assertThat(message.getPayload().getLength().get(), is(appleSize));
    assertThat(copy.getPayload().getLength().get(), is(appleSize));
  }

  private Message createTestMessage() {
    return new DefaultMessageBuilder().value(TEST_PAYLOAD).mediaType(TEXT).build();
  }

  private void assertTestMessage(Message message) {
    assertThat(message.getPayload().getValue(), is(TEST_PAYLOAD));
    assertThat(message.getPayload().getDataType(), is(TEXT_STRING));
    assertThat(message.getAttributes().getValue(), is(nullValue()));
    assertThat(message.getAttributes().getDataType(), is(OBJECT));
  }

}
