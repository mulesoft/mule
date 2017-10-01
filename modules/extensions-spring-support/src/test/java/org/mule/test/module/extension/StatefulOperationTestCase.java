/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.tck.MuleTestUtils.getTestFlow;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.module.extension.internal.util.ExtensionsTestUtils;

import org.junit.Test;

import java.math.BigDecimal;

public class StatefulOperationTestCase extends AbstractExtensionFunctionalTestCase {

  private static final String TENANT_1 = "heisenberg";
  private static final String TENANT_2 = "walter";
  private static final String STATIC_CONFIG = "staticHeisenberg";
  private static final String DYNAMIC_CONFIG = "heisenberg";

  @Override
  protected String getConfigFile() {
    return "heisenberg-stateful-operation-config.xml";
  }

  @Test
  public void stateOnOperationInstance() throws Exception {
    assertThat(dynamicLaunder(), is(40000L));
  }

  @Test
  public void stateOnDynamicConfigs() throws Exception {
    dynamicLaunder();

    assertRemainingMoney(DYNAMIC_CONFIG, TENANT_1, 70000);
    assertRemainingMoney(DYNAMIC_CONFIG, TENANT_2, 90000);
  }

  @Test
  public void stateOnStaticConfig() throws Exception {
    staticLounder(10000);
    staticLounder(5000);

    assertRemainingMoney(STATIC_CONFIG, "", 85000);
  }

  private long dynamicLaunder() throws Exception {
    doDynamicLaunder(TENANT_1, 30000);
    return doDynamicLaunder(TENANT_2, 10000);
  }

  private void assertRemainingMoney(String configName, String name, long expectedAmount) throws Exception {
    CoreEvent event = CoreEvent.builder(create(getTestFlow(muleContext), TEST_CONNECTOR_LOCATION)).message(of(""))
        .addVariable("myName", name).build();

    HeisenbergExtension heisenbergExtension = ExtensionsTestUtils.getConfigurationFromRegistry(configName, event, muleContext);
    assertThat(heisenbergExtension.getMoney(), equalTo(BigDecimal.valueOf(expectedAmount)));
  }

  private long doDynamicLaunder(String name, long amount) throws Exception {
    return (Long) flowRunner("laundry").withPayload(amount).withVariable("myName", name).run().getMessage().getPayload()
        .getValue();
  }

  private long staticLounder(long amount) throws Exception {
    return (Long) flowRunner("staticLaundry").withPayload(amount).run().getMessage().getPayload().getValue();
  }
}
