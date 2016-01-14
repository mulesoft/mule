/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal;

import org.mule.construct.Flow;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.module.extension.HeisenbergExtension;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;

import java.math.BigDecimal;

import org.junit.Test;

public class MessageSourceTestCase extends ExtensionFunctionalTestCase
{

    @Override
    protected Class<?>[] getAnnotatedExtensionClasses()
    {
        return new Class<?>[] {HeisenbergExtension.class};
    }

    @Override
    protected String getConfigFile()
    {
        return "heisenberg-source-config.xml";
    }

    @Test
    public void source() throws Exception
    {
        startFlow("source");
        HeisenbergExtension heisenberg = locateConfig();

        PollingProber prober = new PollingProber(5000, 100);
        prober.check(new JUnitProbe()
        {
            @Override
            protected boolean test() throws Exception
            {
                return new BigDecimal(100).compareTo(heisenberg.getMoney()) < 0;
            }
        });
    }

    @Test
    public void onException() throws Exception
    {
        startFlow("sourceFailed");
        HeisenbergExtension heisenberg = locateConfig();

        PollingProber prober = new PollingProber(5000, 100);
        prober.check(new JUnitProbe()
        {
            @Override
            protected boolean test() throws Exception
            {
                return heisenberg.getMoney().longValue() == -1;
            }
        });
    }

    private HeisenbergExtension locateConfig() throws Exception
    {
        return (HeisenbergExtension) muleContext.getExtensionManager().getConfiguration("heisenberg", getTestEvent("")).getValue();
    }

    private void startFlow(String flowName) throws Exception
    {
        ((Flow) getFlowConstruct(flowName)).start();
    }
}
