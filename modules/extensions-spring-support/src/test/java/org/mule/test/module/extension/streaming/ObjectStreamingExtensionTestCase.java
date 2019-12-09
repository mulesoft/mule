/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.streaming;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mule.test.allure.AllureConstants.StreamingFeature.STREAMING;
import static org.mule.test.allure.AllureConstants.StreamingFeature.StreamingStory.OBJECT_STREAMING;

import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.mule.functional.api.flow.FlowRunner;
import org.mule.runtime.api.streaming.object.CursorIteratorProvider;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.streaming.iterator.ConsumerStreamingIterator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.apache.commons.collections.IteratorUtils;
import org.hamcrest.BaseMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mule.tck.junit4.FlakinessDetectorTestRunner;
import org.mule.tck.junit4.FlakyTest;

@Feature(STREAMING)
@Story(OBJECT_STREAMING)
//@RunWith(FlakinessDetectorTestRunner.class)
public class ObjectStreamingExtensionTestCase extends AbstractStreamingExtensionTestCase {

  private static final int DATA_SIZE = 100;
  private static final String MY_STREAM_VAR = "myStreamVar";
  private List<String> data;

  @Rule
  public ExpectedException expectedException = none();

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    data = new ArrayList<>(DATA_SIZE);
    for (int i = 0; i < DATA_SIZE; i++) {
      data.add(randomAlphabetic(DATA_SIZE));
    }
  }

  @Override
  protected String getConfigFile() {
    return "objects-streaming-extension-config.xml";
  }

  @Test
  @Description("Consume an object stream")
  public void getObjectStream() throws Exception {
    assertStreamMatchesData("getStreamWithoutStreaming");
  }

  @Test
  @Description("Stores an object stream in a variable leaving without modifying the original payload")
  public void getObjectStreamWithTargetValue() throws Exception {
    CoreEvent event = flowRunner("getStreamWithTargetValue").withPayload(data).run();
    assertThat(event.getVariables().get(MY_STREAM_VAR).getValue(), is(instanceOf(String.class)));
    assertThat(event.getVariables().get(MY_STREAM_VAR).getValue(), equalTo(data.get(0)));
  }

  @Test
  @Description("Stores an object stream in a variable leaving without modifying the original payload")
  public void getObjectStreamWithTargetVariable() throws Exception {
    CoreEvent event = flowRunner("getStreamWithTarget").keepStreamsOpen().withPayload(data).run();
    assertThat(event.getVariables().get(MY_STREAM_VAR).getValue(), is(instanceOf(CursorIteratorProvider.class)));
    assertThat(IteratorUtils.toList(((CursorIteratorProvider) event.getVariables().get(MY_STREAM_VAR).getValue()).openCursor()),
               equalTo(data));
    assertThat(event.getMessage().getPayload().getValue(), is(instanceOf(List.class)));
    assertThat(event.getMessage().getPayload().getValue(), equalTo(data));
  }

  @Test
  @Description("Operation is configured not to stream")
  public void operationWithDisabledStreaming() throws Exception {
    assertStreamMatchesData("getStreamWithoutStreaming");
  }

  @Test
  @Description("Operation is configured not to stream and stream gets closed automatically even if not consumed")
  //@FlakyTest(times = 100)
  public void nonRepeatableStreamIsManaged() throws Exception {
    Object stream = getObjectStream("getStreamWithoutStreaming", false);
    assertThat(stream, is(instanceOf(ConsumerStreamingIterator.class)));

    ConsumerStreamingIterator streamingIterator = (ConsumerStreamingIterator) stream;
    assertThat(streamingIterator.hasNext(), is(false));
    expectedException.expect(new BaseMatcher<Throwable>() {

      @Override
      public boolean matches(Object o) {
        return o.getClass().getName().equals("org.mule.runtime.core.internal.streaming.object.iterator.ClosedConsumerException");
      }

      @Override
      public void describeTo(org.hamcrest.Description description) {
        description.appendText("Exception was not a ClosedConsumerException");
      }
    });

    streamingIterator.next();
  }

  @Test
  @Description("Consume a stream generated in a transaction")
  public void getStreamInTx() throws Exception {
    assertStreamMatchesData("getStreamInTx");
  }

  @Test(expected = Exception.class)
  @Description("All cursors closed when the flow fails")
  public void allStreamsClosedInCaseOfException() throws Exception {
    flowRunner("crashCar").withPayload(data).run();
  }

  @Test(expected = Exception.class)
  @Description("All cursors closed when the flow fails in a transaction")
  public void allStreamsClosedInCaseOfExceptionInTx() throws Exception {
    flowRunner("crashCarTx").withPayload(data).run();
  }

  private Object getObjectStream(String flowName, boolean keepStreamsOpen) throws Exception {
    FlowRunner flowRunner = flowRunner(flowName).withPayload(data);

    if (keepStreamsOpen) {
      flowRunner.keepStreamsOpen();
    }

    return flowRunner.run().getMessage().getPayload().getValue();
  }

  private List<String> consumeObjectStream(String flowName, boolean keepStreamsOpen) throws Exception {
    Object stream = getObjectStream(flowName, keepStreamsOpen);
    if (stream instanceof Iterator) {
      Iterator<String> it = (Iterator<String>) stream;
      List<String> list = new LinkedList<>();
      it.forEachRemaining(list::add);

      return list;
    } else if (stream instanceof List) {
      return (List<String>) stream;
    }

    throw new IllegalStateException("Stream of unknown type: " + stream.getClass());
  }

  private void assertStreamMatchesData(String flowName) throws Exception {
    List<String> actual = consumeObjectStream(flowName, true);
    assertThat(actual, equalTo(data));
  }

}
