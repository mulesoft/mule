/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.source;

import static org.mule.tck.probe.PollingProber.check;
import static org.mule.test.petstore.extension.PetStoreSimpleSourceWithSdkApi.ON_ERROR_CALL_COUNT;
import static org.mule.test.petstore.extension.PetStoreSimpleSourceWithSdkApi.ON_SUCCESS_CALL_COUNT;
import static org.mule.test.petstore.extension.PetStoreSimpleSourceWithSdkApi.ON_TERMINATE_CALL_COUNT;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

public class SourceExecutionSdkApiTestCase extends AbstractExtensionFunctionalTestCase {

  private static final int TIMEOUT = 5000;
  private static final int DELAY = 100;

  private static final List<CoreEvent> EVENTS = new LinkedList<>();

  @Override
  protected void doTearDown() throws Exception {
    EVENTS.clear();
  }

  @Override
  protected String getConfigFile() {
    return "source/source-notification-sdk-api-config.xml";
  }

  @Test
  public void onSuccessMethodCall() throws Exception {
    startFlow("onSuccessMethodCallFlow");
    check(TIMEOUT, DELAY, () -> ON_SUCCESS_CALL_COUNT > 0);
    check(TIMEOUT, DELAY, () -> ON_TERMINATE_CALL_COUNT > 0);
  }

  @Test
  public void onErrorMethodCall() throws Exception {
    startFlow("onErrorMethodCallFlow");
    check(TIMEOUT, DELAY, () -> ON_ERROR_CALL_COUNT > 0);
    check(TIMEOUT, DELAY, () -> ON_TERMINATE_CALL_COUNT > 0);
  }

  private void startFlow(String flowName) throws Exception {
    ((Startable) getFlowConstruct(flowName)).start();
  }


  public static class SourceProcessor implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      synchronized (EVENTS) {
        EVENTS.add(event);
      }
      return event;
    }
  }

}
