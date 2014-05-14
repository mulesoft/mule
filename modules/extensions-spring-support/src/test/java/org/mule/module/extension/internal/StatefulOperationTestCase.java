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
import org.mule.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.tck.junit4.ExtensionsFunctionalTestCase;

import java.math.BigDecimal;

import org.junit.Test;

public class StatefulOperationTestCase extends ExtensionsFunctionalTestCase
{

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
    public void statefulOperation() throws Exception
    {
        final String dynamicConfig1 = "heisenberg";
        doLaunder(dynamicConfig1, 10000);
        final long totalLaunderedAmountForConfig1 = doLaunder(dynamicConfig1, 20000);

        final String dynamicConfig2 = "walter";
        doLaunder(dynamicConfig2, 30000);
        final long totalLaunderedAmountForConfig2 = doLaunder(dynamicConfig2, 30000);

        assertThat(totalLaunderedAmountForConfig1, is(30000L));
        assertThat(totalLaunderedAmountForConfig2, is(60000L));

        assertRemainingMoney(dynamicConfig1, 70000);
        assertRemainingMoney(dynamicConfig2, 40000);
    }

    private void assertRemainingMoney(String name, long expectedAmount) throws Exception
    {
        ValueResolver<HeisenbergExtension> configResolver = muleContext.getRegistry().get("heisenberg");
        MuleEvent event = getTestEvent("");
        event.setFlowVariable("myName", name);

        HeisenbergExtension heisenbergExtension = configResolver.resolve(event);
        assertThat(heisenbergExtension.getMoney(), equalTo(BigDecimal.valueOf(expectedAmount)));
    }

    private long doLaunder(String name, long amount) throws Exception
    {
        MuleEvent event = getTestEvent(amount);
        event.setFlowVariable("myName", name);

        return (Long) runFlow("laundry", event).getMessage().getPayload();
    }
}
