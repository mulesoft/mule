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

    private static final String DYNAMIC_CONFIG_1 = "heisenberg";
    private static final String DYNAMIC_CONFIG_2 = "walter";

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
        assertThat(launder(), is(40000L));
    }

    @Test
    public void stateOnDynamicConfigs() throws Exception
    {
        launder();

        assertRemainingMoney(DYNAMIC_CONFIG_1, 70000);
        assertRemainingMoney(DYNAMIC_CONFIG_2, 90000);
    }

    private long launder() throws Exception
    {
        doLaunder(DYNAMIC_CONFIG_1, 30000);
        return doLaunder(DYNAMIC_CONFIG_2, 10000);
    }

    private void assertRemainingMoney(String name, long expectedAmount) throws Exception
    {
        MuleEvent event = getTestEvent("");
        event.setFlowVariable("myName", name);

        HeisenbergExtension heisenbergExtension = ExtensionsTestUtils.getConfigurationInstanceFromRegistry("heisenberg", event);
        assertThat(heisenbergExtension.getMoney(), equalTo(BigDecimal.valueOf(expectedAmount)));
    }

    private long doLaunder(String name, long amount) throws Exception
    {
        MuleEvent event = getTestEvent(amount);
        event.setFlowVariable("myName", name);

        return (Long) runFlow("laundry", event).getMessage().getPayload();
    }
}
