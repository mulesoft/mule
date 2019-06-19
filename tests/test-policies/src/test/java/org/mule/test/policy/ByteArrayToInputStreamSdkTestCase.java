/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.policy;


import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.tck.probe.PollingProber;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

public class ByteArrayToInputStreamSdkTestCase extends MuleArtifactFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "byte-array-to-input-stream-config.xml";
  }

  @Test
  public void byteArrayToInputStreamTransformationIsSuccessful() {
    PollingProber.check(240000, 1000, () -> EventRecorder.countCapturedEvents() == 1);
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
