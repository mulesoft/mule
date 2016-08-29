/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.operation;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mule.runtime.module.extension.internal.util.ExtensionsTestUtils.getConfigurationFromRegistry;

import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.runtime.api.message.MuleEvent;
import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.test.vegan.extension.BananaConfig;
import org.mule.test.vegan.extension.VeganAttributes;
import org.mule.test.vegan.extension.VeganExtension;

import org.junit.Test;

public class InterceptingOperationExecutionTestCase extends ExtensionFunctionalTestCase {

  private BananaConfig config;

  @Override
  protected Class<?>[] getAnnotatedExtensionClasses() {
    return new Class<?>[] {VeganExtension.class};
  }

  @Override
  protected String getConfigFile() {
    return "vegan-intercepting-operation-config.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    config = getConfigurationFromRegistry("banana", getTestEvent(""), muleContext);
  }

  @Test
  public void interceptingWithoutNext() throws Exception {
    MuleMessage message = flowRunner("interceptingWithoutNext").run().getMessage();
    assertThat(message.getPayload(), is(instanceOf(Banana.class)));
    assertThat(message.getAttributes(), is(instanceOf(VeganAttributes.class)));
  }

  @Test
  public void interceptChain() throws Exception {
    MuleMessage message = flowRunner("interceptingBanana").run().getMessage();
    assertThat(message.getPayload(), is(instanceOf(Banana.class)));
    final Banana banana = message.getPayload();

    assertThat(banana.isPeeled(), is(true));
    assertThat(banana.isBitten(), is(true));

    assertThat(config.getBananasCount(), is(1));
    assertThat(config.getNonBananasCount(), is(0));
    assertThat(config.getExceptionCount(), is(0));
  }

  @Test
  public void interceptInvalidPayload() throws Exception {
    MuleMessage message = flowRunner("interceptingNotBanana").run().getMessage();

    assertThat(message.getPayload(), is(not(instanceOf(Fruit.class))));

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
    MuleEvent event = flowRunner("interceptingWithTarget").withPayload(payload).run();
    assertThat(event.getMessage().getPayload(), is(payload));

    MuleMessage message = event.getFlowVariable("banana");
    assertThat(message.getPayload(), is(instanceOf(Banana.class)));
    final Banana banana = message.getPayload();

    assertThat(banana.isPeeled(), is(true));
    assertThat(banana.isBitten(), is(true));

    assertThat(config.getBananasCount(), is(0));
    assertThat(config.getNonBananasCount(), is(1));
    assertThat(config.getExceptionCount(), is(0));
  }

  @Test
  public void nestedInterceptingWithTarget() throws Exception {
    MuleEvent event = flowRunner("nestedInterceptingWithTarget").run();
    assertThat(event.getMessage().getPayload(), is(instanceOf(Banana.class)));

    MuleMessage targetMessage = event.getFlowVariable("banana");
    assertThat(targetMessage.getPayload(), is(instanceOf(Banana.class)));

    assertThat(event.getMessage().getPayload(), is(not(sameInstance(targetMessage.getPayload()))));

    assertThat(config.getBananasCount(), is(2));
    assertThat(config.getNonBananasCount(), is(0));
    assertThat(config.getExceptionCount(), is(0));
  }

  @Test
  public void interceptingWithoutGenerics() throws Exception {
    MuleEvent event = flowRunner("InterceptingWithoutGenerics").run();
    assertThat(event.getMessage().getPayload(), is(instanceOf(Banana.class)));
  }
}
