/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.message;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.mockito.Mockito.mock;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.tck.size.SmallTest;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

@SmallTest
public class ResultToMessageIteratorTestCase {

  @Test
  public void iteratesOverAllElements() {
    ResultToMessageIterator resultToMessageIterator = createResultToMessageIterator();
    List<Integer> outputList = new ArrayList<>();

    while (resultToMessageIterator.hasNext()) {
      outputList.add((Integer) resultToMessageIterator.next().getPayload().getValue());
    }

    assertOutput(outputList);
  }

  @Test
  public void forEachRemainingIteratesOverAllElements() {
    ResultToMessageIterator resultToMessageIterator = createResultToMessageIterator();
    List<Integer> outputList = new ArrayList<>();

    resultToMessageIterator.forEachRemaining(m -> outputList.add((Integer) m.getPayload().getValue()));

    assertOutput(outputList);
  }

  private void assertOutput(List<Integer> outputList) {
    assertThat(outputList, hasItems(1, 2, 3, 4, 5));
  }

  private ResultToMessageIterator createResultToMessageIterator() {
    List<Object> list = new ArrayList<>();
    list.add(resultOf(1));
    list.add(resultOf(2));
    list.add(resultOf(3));
    list.add(resultOf(4));
    list.add(resultOf(5));

    CursorProviderFactory cursorProviderFactory = mock(CursorProviderFactory.class);
    CoreEvent coreEvent = mock(CoreEvent.class);

    return new ResultToMessageIterator(list.iterator(), cursorProviderFactory, coreEvent);
  }

  private static Result<Object, Object> resultOf(int output) {
    return Result.builder().output(output).build();
  }
}
