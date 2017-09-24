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
import static org.mule.test.allure.AllureConstants.StreamingFeature.STREAMING;
import static org.mule.test.allure.AllureConstants.StreamingFeature.StreamingStory.OBJECT_STREAMING;
import org.mule.runtime.api.streaming.object.CursorIteratorProvider;
import org.mule.runtime.core.api.event.CoreEvent;

import java.util.ArrayList;
import java.util.List;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.apache.commons.collections.IteratorUtils;
import org.junit.Test;

@Feature(STREAMING)
@Story(OBJECT_STREAMING)
public class ObjectStreamingExtensionTestCase extends AbstractStreamingExtensionTestCase {

  private static final int DATA_SIZE = 100;
  private static final String MY_STREAM_VAR = "myStreamVar";
  private List<String> data;

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
    assertStreamMatchesData("getStream");
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

  private List<String> getStream(String flowName) throws Exception {
    CoreEvent result = flowRunner(flowName)
        .withPayload(data)
        .run();

    return (List<String>) result.getMessage().getPayload().getValue();
  }

  private void assertStreamMatchesData(String flowName) throws Exception {
    List<String> actual = getStream(flowName);
    assertThat(actual, equalTo(data));
  }

}
