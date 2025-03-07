/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.message;

import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.api.metadata.DataType.BOOLEAN;
import static org.mule.runtime.api.metadata.DataType.JSON_STRING;
import static org.mule.runtime.api.metadata.DataType.OBJECT;
import static org.mule.runtime.api.metadata.DataType.STRING;
import static org.mule.runtime.api.metadata.DataType.TEXT_STRING;
import static org.mule.runtime.api.metadata.DataType.fromObject;
import static org.mule.runtime.api.metadata.MediaType.ANY;
import static org.mule.runtime.api.metadata.MediaType.APPLICATION_JSON;
import static org.mule.runtime.api.metadata.MediaType.HTML;
import static org.mule.runtime.api.metadata.MediaType.TEXT;
import static org.mule.tck.junit4.matcher.DataTypeCompatibilityMatcher.assignableTo;

import static java.nio.charset.StandardCharsets.UTF_8;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.privileged.metadata.DefaultCollectionDataType;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.Apple;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;

import org.junit.Test;

public class DefaultMuleMessageBuilderTestCase extends AbstractMuleTestCase {

  private static final String NEW_PAYLOAD = "new payload";
  private static final String EMPTY_JSON = "{}";
  private static final Object BASE_ATTRIBUTES = new BaseAttributes() {};
  private static final DataType BASE_ATTRIBUTES_DATATYPE = fromObject(BASE_ATTRIBUTES);
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
  public void copyPreservesDataType() {
    Apple apple = new Apple();
    long appleSize = 111;
    Message message =
        new DefaultMessageBuilder().payload(new TypedValue<>(apple, fromObject(apple), OptionalLong.of(appleSize))).build();
    Message copy = new DefaultMessageBuilder(message).build();

    assertThat(copy.getPayload(), is(message.getPayload()));
    assertThat(copy.getAttributes(), is(message.getAttributes()));
    assertThat(message.getPayload().getByteLength().getAsLong(), is(appleSize));
    assertThat(copy.getPayload().getByteLength().getAsLong(), is(appleSize));
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
