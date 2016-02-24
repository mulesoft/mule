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
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

import java.math.BigDecimal;

import org.junit.Test;

public class MessageSourceTestCase extends ExtensionFunctionalTestCase
{

    public static final int TIMEOUT_MILLIS = 5000;
    public static final int POLL_DELAY_MILLIS = 100;

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

        new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS).check(new JUnitLambdaProbe(() -> new BigDecimal(POLL_DELAY_MILLIS).compareTo(heisenberg.getMoney()) < 0));
    }

    @Test
    public void onException() throws Exception
    {
        startFlow("sourceFailed");
        HeisenbergExtension heisenberg = locateConfig();

        new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS).check(new JUnitLambdaProbe(() -> heisenberg.getMoney().longValue() == -1));
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
