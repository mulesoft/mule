/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.reconnection;

import static org.mule.tck.probe.PollingProber.check;
import org.mule.extension.test.extension.reconnection.ReconnectableConnection;
import org.mule.extension.test.extension.reconnection.ReconnectableConnectionProvider;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import java.util.LinkedList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class ReconnectionTestCase extends AbstractExtensionFunctionalTestCase {

  private static List<CoreEvent> capturedEvents;

  public static class CaptureProcessor implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      synchronized (capturedEvents) {
        capturedEvents.add(event);
      }
      return event;
    }
  }

  @Override
  protected String getConfigFile() {
    return "reconnection-config.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    capturedEvents = new LinkedList<>();
    ReconnectableConnectionProvider.fail = false;
  }

  @Override
  protected void doTearDown() throws Exception {
    capturedEvents = null;
    ReconnectableConnectionProvider.fail = false;
  }

  @Test
  public void reconnectSource() throws Exception {
    ((Startable) getFlowConstruct("reconnectForever")).start();
    check(5000, 1000, () -> !capturedEvents.isEmpty());
    switchConnection();

    check(10000, 1000, () -> {
      synchronized (capturedEvents) {
        return capturedEvents.stream()
            .map(event -> (ReconnectableConnection) event.getMessage().getPayload().getValue())
            .filter(c -> c.getReconnectionAttempts() >= 3)
            .findAny()
            .isPresent();
      }
    });
  }

  private void switchConnection() throws Exception {
    flowRunner("switchConnection").run();
  }
}
