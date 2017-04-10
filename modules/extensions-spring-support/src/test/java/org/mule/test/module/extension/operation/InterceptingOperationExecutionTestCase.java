/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.operation;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.getConfigurationFromRegistry;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.vegan.extension.BananaConfig;
import org.mule.test.vegan.extension.VeganAttributes;

import org.junit.Test;

public class InterceptingOperationExecutionTestCase extends AbstractExtensionFunctionalTestCase {

  private BananaConfig config;

  @Override
  protected String getConfigFile() {
    return "vegan-intercepting-operation-config.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    config = getConfigurationFromRegistry("banana", testEvent(), muleContext);
  }

  @Test
  public void interceptingWithoutNext() throws Exception {
    Message message = flowRunner("interceptingWithoutNext").run().getMessage();
    assertThat(message.getPayload().getValue(), is(instanceOf(Banana.class)));
    assertThat(message.getAttributes(), is(instanceOf(VeganAttributes.class)));
  }

  @Test
  public void interceptChain() throws Exception {
    Message message = flowRunner("interceptingBanana").run().getMessage();
    assertThat(message.getPayload().getValue(), is(instanceOf(Banana.class)));
    final Banana banana = (Banana) message.getPayload().getValue();

    assertThat(banana.isPeeled(), is(true));
    assertThat(banana.isBitten(), is(true));

    assertThat(config.getBananasCount(), is(1));
    assertThat(config.getNonBananasCount(), is(0));
    assertThat(config.getExceptionCount(), is(0));
  }

  @Test
  public void interceptInvalidPayload() throws Exception {
    Message message = flowRunner("interceptingNotBanana").run().getMessage();

    assertThat(message.getPayload().getValue(), is(not(instanceOf(Fruit.class))));

    assertThat(config.getBananasCount(), is(0));
    assertThat(config.getNonBananasCount(), is(1));
    assertThat(config.getExceptionCount(), is(0));
  }

  @Test
  public void interceptError() throws Exception {
    try {
      flowRunner("interceptingError").run();
      fail("Flow should have failed");
    } catch (MessagingException e) {
      assertThat(config.getExceptionCount(), is(1));
    }
  }

  @Test
  public void interceptingWithTarget() throws Exception {
    final String payload = "Hello!";
    Event event = flowRunner("interceptingWithTarget").withPayload(payload).run();
    assertThat(event.getMessage().getPayload().getValue(), is(payload));

    Message message = (Message) event.getVariable("banana").getValue();
    assertThat(message.getPayload().getValue(), is(instanceOf(Banana.class)));
    final Banana banana = (Banana) message.getPayload().getValue();

    assertThat(banana.isPeeled(), is(true));
    assertThat(banana.isBitten(), is(true));

    assertThat(config.getBananasCount(), is(0));
    assertThat(config.getNonBananasCount(), is(1));
    assertThat(config.getExceptionCount(), is(0));
  }

  @Test
  public void nestedInterceptingWithTarget() throws Exception {
    Event event = flowRunner("nestedInterceptingWithTarget").run();
    assertThat(event.getMessage().getPayload().getValue(), is(instanceOf(Banana.class)));

    Message targetMessage = (Message) event.getVariable("banana").getValue();
    assertThat(targetMessage.getPayload().getValue(), is(instanceOf(Banana.class)));

    assertThat(event.getMessage().getPayload().getValue(), is(not(sameInstance(targetMessage.getPayload().getValue()))));

    assertThat(config.getBananasCount(), is(2));
    assertThat(config.getNonBananasCount(), is(0));
    assertThat(config.getExceptionCount(), is(0));
  }

  @Test
  public void interceptingWithoutGenerics() throws Exception {
    Event event = flowRunner("InterceptingWithoutGenerics").run();
    assertThat(event.getMessage().getPayload().getValue(), is(instanceOf(Banana.class)));
  }
}
