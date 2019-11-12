/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.metadata.DataType.STRING;
import static org.mule.runtime.api.metadata.MediaType.APPLICATION_JSON;
import static org.mule.runtime.core.api.util.StreamingUtils.consumeRepeatablePayload;
import static org.mule.runtime.core.api.util.StreamingUtils.consumeRepeatableValue;
import static org.mule.test.allure.AllureConstants.StreamingFeature.STREAMING;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.CollectionDataType;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.api.streaming.object.CursorIteratorProvider;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.streaming.bytes.InMemoryCursorStreamConfig;
import org.mule.runtime.core.api.streaming.bytes.InMemoryCursorStreamProvider;
import org.mule.runtime.core.api.streaming.iterator.StreamingIterator;
import org.mule.runtime.core.api.streaming.object.InMemoryCursorIteratorConfig;
import org.mule.runtime.core.internal.streaming.bytes.ByteArrayCursorStreamProvider;
import org.mule.runtime.core.internal.streaming.object.InMemoryCursorIteratorProvider;
import org.mule.runtime.core.internal.streaming.bytes.SimpleByteBufferManager;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@SmallTest
@Feature(STREAMING)
@Story(STREAMING)
public class StreamingUtilsTestCase extends AbstractMuleTestCase {

  private static final List<String> TEST_LIST = Arrays.asList("Apple", "Banana", "Kiwi");

  @Test
  @Description("Test that repeatable stream in the payload is consumed into another fully in memory stream provider")
  public void consumeRepeatableInputStreamPayload() throws Exception {
    CursorStreamProvider payload = asCursorProvider(TEST_PAYLOAD);
    CoreEvent event = consumeRepeatablePayload(getEventBuilder().message(Message.of(payload)).build());
    assertConsumedRepeatableInputStream(payload, event);
  }

  @Test
  @Description("Test that repeatable stream in the typed value is consumed into another fully in memory stream provider")
  public void consumeRepeatableInputStreamTypedValue() throws Exception {
    CursorStreamProvider payload = asCursorProvider(TEST_PAYLOAD);
    TypedValue consumed = consumeRepeatableValue(TypedValue.of(payload));
    assertConsumedRepeatableInputStream(payload, consumed);
  }

  @Test
  @Description("Test that repeatable stream in the payload is consumed into another fully in memory stream provider while maintaining "
      + "the original media type")
  public void consumeJsonRepeatableInputStreamPayload() throws Exception {
    CursorStreamProvider payload = asCursorProvider(TEST_PAYLOAD);
    CoreEvent event = consumeRepeatablePayload(getEventBuilder().message(Message.builder()
        .payload(TypedValue.of(payload))
        .mediaType(APPLICATION_JSON)
        .build())
        .build());
    assertConsumedRepeatableInputStream(payload, event);
    assertThat(event.getMessage().getPayload().getDataType().getMediaType(), is(APPLICATION_JSON));
  }

  @Test
  @Description("Test that repeatable stream in the typed value is consumed into another fully in memory stream provider while "
      + "maintaining the original media type")
  public void consumeJsonRepeatableInputStreamTypedValue() throws Exception {
    CursorStreamProvider payload = asCursorProvider(TEST_PAYLOAD);
    TypedValue original =
        new TypedValue(payload, DataType.builder().type(payload.getClass()).mediaType(APPLICATION_JSON).build());
    TypedValue consumed = consumeRepeatableValue(original);

    assertConsumedRepeatableInputStream(payload, consumed);
    assertThat(consumed.getDataType().getMediaType(), is(APPLICATION_JSON));
  }

  @Test
  @Description("Test that repeatable iterator is consumed into a list")
  public void consumeRepeatableIteratorPayload() throws Exception {
    CursorIteratorProvider payload = asCursorProvider(TEST_LIST);
    CoreEvent event = consumeRepeatablePayload(getEventBuilder().message(Message.of(payload)).build());
    assertConsumedObjectStream(payload, event);
  }

  @Test
  @Description("Test that repeatable iterator in typed value is consumed into a list")
  public void consumeRepeatableIteratorTypedValue() throws Exception {
    CursorIteratorProvider payload = asCursorProvider(TEST_LIST);
    TypedValue consumed = consumeRepeatableValue(TypedValue.of(payload));
    assertConsumedObjectStream(payload, consumed);
  }

  @Test
  @Description("Test that repeatable iterator is consumed into a list while maintaining the collection data type")
  public void consumeTypedRepeatableIteratorPayload() throws Exception {
    CursorIteratorProvider payload = asCursorProvider(TEST_LIST);
    DataType dataType = DataType.builder().collectionType(ArrayList.class).itemType(String.class).build();

    CoreEvent event = consumeRepeatablePayload(getEventBuilder().message(Message.builder()
        .payload(new TypedValue<>(payload, dataType))
        .build())
        .build());

    assertConsumedObjectStream(payload, event);

    dataType = event.getMessage().getPayload().getDataType();
    assertThat(dataType, is(instanceOf(CollectionDataType.class)));
    assertThat(((CollectionDataType) dataType).getItemDataType(), equalTo(STRING));
  }

  @Test
  @Description("Test that repeatable iterator in typed value is consumed into a list while maintaining the collection data type")
  public void consumeTypedRepeatableIteratorTypedValue() throws Exception {
    CursorIteratorProvider payload = asCursorProvider(TEST_LIST);
    TypedValue original = new TypedValue(payload, DataType.builder()
        .collectionType(ArrayList.class)
        .itemType(String.class)
        .build());

    TypedValue consumed = consumeRepeatableValue(original);

    assertConsumedObjectStream(payload, consumed);

    DataType dataType = consumed.getDataType();
    assertThat(dataType, is(instanceOf(CollectionDataType.class)));
    assertThat(((CollectionDataType) dataType).getItemDataType(), equalTo(STRING));
  }

  @Test
  @Description("Test that event without repeatable stream payload is not modified")
  public void dontConsumeUnrepeatableInputStreamPayload() throws Exception {
    CoreEvent event = getEventBuilder().message(Message.of(TEST_PAYLOAD)).build();
    assertThat(consumeRepeatablePayload(event), is(sameInstance(event)));
  }

  @Test
  @Description("Test that typed value without repeatable stream payload is not modified")
  public void dontConsumeUnrepeatableInputStreamTypedValue() throws Exception {
    TypedValue value = TypedValue.of(TEST_PAYLOAD);
    assertThat(consumeRepeatableValue(value), is(sameInstance(value)));
  }

  @Test
  @Description("Test that event without repeatable iterator payload is not modified")
  public void dontConsumeUnrepeatableIteratorPayload() throws Exception {
    CoreEvent event = getEventBuilder().message(Message.of(TEST_LIST)).build();
    assertThat(consumeRepeatablePayload(event), is(sameInstance(event)));
  }

  @Test
  @Description("Test that typed value without repeatable iterator payload is not modified")
  public void dontConsumeUnrepeatableIteratorTypedValue() throws Exception {
    TypedValue value = TypedValue.of(TEST_LIST);
    assertThat(consumeRepeatableValue(value), is(sameInstance(value)));
  }

  @Test
  public void consumeEventWithNullPayload() throws Exception {
    CoreEvent event = getEventBuilder().message(Message.of(null)).build();
    assertThat(consumeRepeatablePayload(event), is(sameInstance(event)));
  }

  @Test
  public void consumeNullTypedValue() throws Exception {
    TypedValue value = TypedValue.of(null);
    assertThat(consumeRepeatableValue(value), is(sameInstance(value)));
  }

  private void assertConsumedRepeatableInputStream(CursorStreamProvider payload, CoreEvent event) {
    assertConsumedRepeatableInputStream(payload, event.getMessage().getPayload());
  }

  private void assertConsumedRepeatableInputStream(CursorStreamProvider payload, TypedValue value) {
    Object responsePayload = value.getValue();
    assertThat(responsePayload, is(not(sameInstance(payload))));
    assertThat(responsePayload, is(instanceOf(ByteArrayCursorStreamProvider.class)));
    assertThat(IOUtils.toString((CursorStreamProvider) responsePayload), equalTo(TEST_PAYLOAD));
  }

  private void assertConsumedObjectStream(CursorIteratorProvider payload, CoreEvent event) {
    assertConsumedObjectStream(payload, event.getMessage().getPayload());
  }

  private void assertConsumedObjectStream(CursorIteratorProvider payload, TypedValue value) {
    Object responsePayload = value.getValue();
    assertThat(responsePayload, is(not(sameInstance(payload))));
    assertThat(responsePayload, is(instanceOf(List.class)));
    assertThat(responsePayload, equalTo(TEST_LIST));
  }

  private CursorStreamProvider asCursorProvider(String value) {
    return new InMemoryCursorStreamProvider(new ByteArrayInputStream(value.getBytes()),
                                            InMemoryCursorStreamConfig.getDefault(),
                                            new SimpleByteBufferManager());
  }

  private <T> CursorIteratorProvider asCursorProvider(List<T> list) {
    StreamingIterator<T> iterator = mock(StreamingIterator.class);
    Iterator<T> original = list.iterator();
    when(iterator.hasNext()).thenAnswer(i -> original.hasNext());
    when(iterator.next()).thenAnswer(i -> original.next());

    return new InMemoryCursorIteratorProvider(iterator, InMemoryCursorIteratorConfig.getDefault());
  }
}
