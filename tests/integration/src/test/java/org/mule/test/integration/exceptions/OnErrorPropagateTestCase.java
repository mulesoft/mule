/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.functional.exceptions.FunctionalTestException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.routing.RoutingException;
import org.mule.runtime.core.util.concurrent.Latch;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Error Handling")
@Stories("On Error Propagate")
public class OnErrorPropagateTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/on-error-propagate-use-case-config.xml";
  }

  @Test
  public void typeMatch() throws Exception {
    verifyFlow("onErrorPropagateTypeMatch");
    Optional<Message> customPath = muleContext.getClient().request("queue://custom1", RECEIVE_TIMEOUT).getRight();
    assertThat(customPath.isPresent(), is(false));
    Optional<Message> anyPath = muleContext.getClient().request("queue://any1", RECEIVE_TIMEOUT).getRight();
    assertThat(anyPath.isPresent(), is(false));
  }

  @Test
  public void typeMatchAny() throws Exception {
    verifyFlow("onErrorPropagateTypeMatchAny");
    Optional<Message> customPath = muleContext.getClient().request("queue://custom2", RECEIVE_TIMEOUT).getRight();
    assertThat(customPath.isPresent(), is(false));
  }

  @Test
  public void typeMatchSeveral() throws Exception {
    verifyFlow("onErrorPropagateTypeMatchSeveral", true);
    Optional<Message> anyPath = muleContext.getClient().request("queue://any", RECEIVE_TIMEOUT).getRight();
    assertThat(anyPath.isPresent(), is(false));
    verifyFlow("onErrorPropagateTypeMatchSeveral", false);
    anyPath = muleContext.getClient().request("queue://any", RECEIVE_TIMEOUT).getRight();
    assertThat(anyPath.isPresent(), is(false));

  }

  private void verifyFlow(String flowName, Object payload) throws InterruptedException {
    try {
      flowRunner(flowName).withPayload(payload).dispatch();
    } catch (Exception e) {
      assertThat(e.getCause(), is(instanceOf(FunctionalTestException.class)));
      if (!CallMessageProcessor.latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS)) {
        fail("custom message processor wasn't call");
      }
    }
  }

  private void verifyFlow(String flowName) throws InterruptedException {
    verifyFlow(flowName, TEST_MESSAGE);
  }

  public static class CallMessageProcessor implements Processor {

    public static Latch latch = new Latch();

    @Override
    public Event process(Event event) throws MuleException {
      latch.release();
      return event;
    }
  }

  public static class FailingProcessor implements Processor {

    @Override
    public Event process(Event event) throws MuleException {
      throw new RoutingException(createStaticMessage("Error."), this);
    }

  }

}
