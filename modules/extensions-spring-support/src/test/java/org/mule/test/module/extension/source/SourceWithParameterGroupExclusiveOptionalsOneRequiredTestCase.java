/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.source;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mule.tck.probe.PollingProber.check;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.some.extension.ComplexParameter;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

public class SourceWithParameterGroupExclusiveOptionalsOneRequiredTestCase extends AbstractExtensionFunctionalTestCase {

  private static final int TIMEOUT = 5000;
  private static final int DELAY = 1000;
  private static final List<CoreEvent> EVENTS = new LinkedList<>();

  public static class CaptureProcessor implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      synchronized (EVENTS) {
        EVENTS.add(event);
      }
      return event;
    }
  }

  @Override
  protected void doTearDown() throws Exception {
    EVENTS.clear();
  }

  @Override
  protected String getConfigFile() {
    return "values/some-source-parameter-group.xml";
  }

  @Test
  public void testWithSimpleParameter() throws Exception {
    startFlow("someParameter");
    assertEventsFired();
    assertThat(EVENTS.get(0).getMessage().getPayload().getValue(), is("hello dog!"));
  }

  @Test
  public void testWithComplexParameter() throws Exception {
    startFlow("complexParameter");
    assertEventsFired();
    assertThat(((ComplexParameter) EVENTS.get(0).getMessage().getPayload().getValue()).getAnotherParameter(),
               is("hello bird!"));
  }

  @Test
  public void testWithSimpleParameterDslTrue() throws Exception {
    startFlow("someParameterDslTrue");
    assertEventsFired();
    assertThat(EVENTS.get(0).getMessage().getPayload().getValue(), is("hello dog!"));
  }

  @Test
  public void testWithComplexParameterDslTrue() throws Exception {
    startFlow("complexParameterDslTrue");
    assertEventsFired();
    assertThat(((ComplexParameter) EVENTS.get(0).getMessage().getPayload().getValue()).getAnotherParameter(),
               is("hello bird!"));
  }

  private void startFlow(String flowName) throws Exception {
    ((Startable) getFlowConstruct(flowName)).start();
  }

  private void assertEventsFired() {
    check(TIMEOUT, DELAY, () -> {
      synchronized (EVENTS) {
        return EVENTS.size() >= 1;
      }
    });
  }

}
