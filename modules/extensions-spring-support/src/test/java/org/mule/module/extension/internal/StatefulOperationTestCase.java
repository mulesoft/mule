/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleEvent;
import org.mule.module.extension.HeisenbergExtension;
import org.mule.module.extension.internal.util.ExtensionsTestUtils;
import org.mule.tck.junit4.ExtensionsFunctionalTestCase;

import java.math.BigDecimal;

import org.junit.Test;

public class StatefulOperationTestCase extends ExtensionsFunctionalTestCase
{

    private static final String TENANT_1 = "heisenberg";
    private static final String TENANT_2 = "walter";
    private static final String STATIC_CONFIG = "staticHeisenberg";
    private static final String DYNAMIC_CONFIG = "heisenberg";

    @Override
    protected Class<?>[] getAnnotatedExtensionClasses()
    {
        return new Class<?>[] {HeisenbergExtension.class};
    }

    @Override
    protected String getConfigFile()
    {
        return "heisenberg-stateful-operation-config.xml";
    }

    @Test
    public void stateOnOperationInstance() throws Exception
    {
        assertThat(dynamicLaunder(), is(40000L));
    }

    @Test
    public void stateOnDynamicConfigs() throws Exception
    {
        dynamicLaunder();

        assertRemainingMoney(DYNAMIC_CONFIG, TENANT_1, 70000);
        assertRemainingMoney(DYNAMIC_CONFIG, TENANT_2, 90000);
    }

    @Test
    public void stateOnStaticConfig() throws Exception
    {
        staticLounder(10000);
        staticLounder(5000);

        assertRemainingMoney(STATIC_CONFIG, "", 85000);
    }

    private long dynamicLaunder() throws Exception
    {
        doDynamicLaunder(TENANT_1, 30000);
        return doDynamicLaunder(TENANT_2, 10000);
    }

    private void assertRemainingMoney(String configName, String name, long expectedAmount) throws Exception
    {
        MuleEvent event = getTestEvent("");
        event.setFlowVariable("myName", name);

        HeisenbergExtension heisenbergExtension = ExtensionsTestUtils.getConfigurationInstanceFromRegistry(configName, event);
        assertThat(heisenbergExtension.getMoney(), equalTo(BigDecimal.valueOf(expectedAmount)));
    }

    private long doDynamicLaunder(String name, long amount) throws Exception
    {
        MuleEvent event = getTestEvent(amount);
        event.setFlowVariable("myName", name);

        return (Long) runFlow("laundry", event).getMessage().getPayload();
    }

    private long staticLounder(long amount) throws Exception
    {
        return (Long) runFlow("staticLaundry", getTestEvent(amount)).getMessage().getPayload();
    }
}
