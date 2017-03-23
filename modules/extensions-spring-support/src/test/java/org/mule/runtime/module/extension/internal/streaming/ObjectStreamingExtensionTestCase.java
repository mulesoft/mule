/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.streaming;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import org.mule.runtime.core.api.Event;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class ObjectStreamingExtensionTestCase extends AbstractStreamingExtensionTestCase {

  private static final int DATA_SIZE = 100;
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
  public void getObjectStream() throws Exception {
    assertStreamMatchesData("getStream");
  }

  @Test
  public void operationWithDisabledStreaming() throws Exception {
    assertStreamMatchesData("getStreamWithoutStreaming");
  }

  @Test
  public void getStreamInTx() throws Exception {
    assertStreamMatchesData("getStreamInTx");
  }

  @Test(expected = Exception.class)
  public void allStreamsClosedInCaseOfException() throws Exception {
    flowRunner("crashCar").withPayload(data).run();
  }

  @Test(expected = Exception.class)
  public void allStreamsClosedInCaseOfExceptionInTx() throws Exception {
    flowRunner("crashCarTx").withPayload(data).run();
  }

  private List<String> getStream(String flowName) throws Exception {
    Event result = flowRunner(flowName)
        .withPayload(data)
        .run();

    return (List<String>) result.getMessage().getPayload().getValue();
  }

  private void assertStreamMatchesData(String flowName) throws Exception {
    List<String> actual = getStream(flowName);
    assertThat(actual, equalTo(data));
  }

}
