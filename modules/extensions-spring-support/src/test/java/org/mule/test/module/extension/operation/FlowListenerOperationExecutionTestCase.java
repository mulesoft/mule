/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.operation;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.getConfigurationFromRegistry;

import org.mule.runtime.api.message.Message;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.runner.RunnerDelegateTo;
import org.mule.test.vegan.extension.BananaConfig;

import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized;

@RunnerDelegateTo(Parameterized.class)
public class FlowListenerOperationExecutionTestCase extends AbstractExtensionFunctionalTestCase {

  private BananaConfig config;

  @Parameterized.Parameter(0)
  public String parameterizationName;

  @Parameterized.Parameter(1)
  public String configName;

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {"Using Extensions API", "vegan-flow-listener-operation-config.xml"},
        {"Using SDK API", "sdk-vegan-flow-listener-operation-config.xml"},
    });
  }

  @Override
  protected String getConfigFile() {
    return configName;
  }

  @Override
  protected void doSetUp() throws Exception {
    config = getConfigurationFromRegistry("banana", testEvent(), muleContext);
  }

  @Test
  public void listenSuccessfulFlow() throws Exception {
    Message message = flowRunner("listenAndByteBanana").run().getMessage();
    assertThat(message.getPayload().getValue(), is(instanceOf(Banana.class)));
    final Banana banana = (Banana) message.getPayload().getValue();

    check(() -> {
      assertThat(banana.isPeeled(), is(true));
      assertThat(banana.isBitten(), is(true));

      assertThat(config.getBananasCount(), is(1));
      assertThat(config.getNonBananasCount(), is(0));
      assertThat(config.getExceptionCount(), is(0));
    });
  }

  @Test
  public void listenSuccessfulFlowWithUnexpecteResult() throws Exception {
    Message message = flowRunner("listenAndEatMeat").run().getMessage();

    assertThat(message.getPayload().getValue(), is(not(instanceOf(Fruit.class))));

    check(() -> {
      assertThat(config.getBananasCount(), is(0));
      assertThat(config.getNonBananasCount(), is(1));
      assertThat(config.getExceptionCount(), is(0));
    });
  }

  @Test
  public void listenError() throws Exception {
    flowRunner("listenAndFail").runExpectingException();
    check(() -> assertThat(config.getExceptionCount(), is(1)));
  }

  private void check(Runnable probe) {
    PollingProber.check(5000, 100, () -> {
      probe.run();
      return true;
    });
  }

  public static Fruit bite(Fruit payload) {
    payload.bite();
    return payload;
  }
}
