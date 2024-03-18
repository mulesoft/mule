/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.message;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.core.internal.util.collection.TransformingIterator.from;
import static org.mule.runtime.core.internal.util.message.MessageUtils.toMessage;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.from;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.internal.util.collection.TransformingIterator;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.sdk.api.runtime.operation.Result;
import org.mule.tck.size.SmallTest;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

@SmallTest
public class TransformingMessageIteratorBySdkResultTestCase {

  @Test
  public void iteratesOverAllElements() {
    TransformingIterator<Message> transformingMessageIterator = createResultToMessageIterator();
    List<Integer> outputList = new ArrayList<>();

    while (transformingMessageIterator.hasNext()) {
      outputList.add((Integer) transformingMessageIterator.next().getPayload().getValue());
    }

    assertOutput(outputList);
  }

  @Test
  public void forEachRemainingIteratesOverAllElements() {
    TransformingIterator<Message> transformingMessageIterator = createResultToMessageIterator();
    List<Integer> outputList = new ArrayList<>();

    transformingMessageIterator.forEachRemaining(m -> outputList.add((Integer) m.getPayload().getValue()));

    assertOutput(outputList);
  }

  private void assertOutput(List<Integer> outputList) {
    assertThat(outputList, hasItems(1, 2, 3, 4, 5));
  }

  private TransformingIterator<Message> createResultToMessageIterator() {
    List<Object> list = new ArrayList<>();
    list.add(resultOf(1));
    list.add(resultOf(2));
    list.add(resultOf(3));
    list.add(resultOf(4));
    list.add(resultOf(5));

    CursorProviderFactory cursorProviderFactory = mock(CursorProviderFactory.class);
    BaseEventContext eventCtx = mock(BaseEventContext.class);

    return from(list.iterator(), value -> toMessage((Result) value, cursorProviderFactory, eventCtx, from("logger")));
  }

  private static Result<Object, Object> resultOf(int output) {
    return Result.builder().output(output).build();
  }
}
