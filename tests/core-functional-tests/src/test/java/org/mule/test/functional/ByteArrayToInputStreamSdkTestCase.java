/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.functional;


import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.tck.probe.PollingProber.check;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;

import java.util.LinkedList;
import java.util.List;

import io.qameta.allure.Issue;
import org.junit.Test;
import org.mule.runtime.api.metadata.TypedValue;

public class ByteArrayToInputStreamSdkTestCase extends MuleArtifactFunctionalTestCase {

  private static final int POLL_TIMEOUT_MILLIS = 5000;
  private static final int POLL_DELAY_MILLIS = 1000;
  private static final int EXPECTED_EVENT_COUNT = 1;

  @Override
  protected String getConfigFile() {
    return "byte-array-to-input-stream-config.xml";
  }

  @Test
  public void byteArrayToInputStreamTransformationIsSuccessful() {
    check(POLL_TIMEOUT_MILLIS, POLL_DELAY_MILLIS, () -> EventRecorder.countCapturedEvents() == EXPECTED_EVENT_COUNT);
  }

  @Test
  @Issue("MULE-19279")
  public void byteArrayToInputStreamTransformationIsSuccessfulWithUTF16() throws Exception {
    // Test with UTF-16 character
    String testMessage = "Ź";
    CoreEvent event = flowRunner("transformer").withPayload(testMessage.getBytes("UTF-16")).run();
    TypedValue<String> value = event.getMessage().getPayload();
    assertThat(value.getValue(), is(testMessage));
  }

  public static class EventRecorder implements Processor, Startable {

    private static List<CoreEvent> capturedEvents = new LinkedList();

    public EventRecorder() {}

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      capturedEvents.add(event);
      return event;
    }

    @Override
    public void start() throws MuleException {
      capturedEvents.clear();
    }

    public static int countCapturedEvents() {
      return capturedEvents.size();
    }
  }
}
