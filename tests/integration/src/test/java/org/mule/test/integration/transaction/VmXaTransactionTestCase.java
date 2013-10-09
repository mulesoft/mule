/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.transaction;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;

import org.junit.Rule;
import org.junit.Test;

/**
 *
 */
public class VmXaTransactionTestCase extends FunctionalTestCase
{

    @Rule
    public SystemProperty inboundQueueMaxOutstandingMessages = new SystemProperty("maxOutstandingMessages","50");

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/transaction/vm-xa-transaction-config.xml";
    }

    @Override
    public int getTestTimeoutSecs()
    {
        return 100000;
    }

    @Test
    public void test() throws MuleException, InterruptedException
    {
        for (int i = 0; i < new Integer(inboundQueueMaxOutstandingMessages.getValue()); i++)
        {
            System.out.println("Sending message: " + i);
            muleContext.getClient().dispatch("vm://input?connector=vm-connector-large","message",null);
        }
        Thread.sleep(10000);
        for (int i = 0; i < new Integer(inboundQueueMaxOutstandingMessages.getValue()); i++)
        {
            PollingProber prober = new PollingProber(10000, 100);
            prober.check(new Probe()
            {
                public boolean isSatisfied()
                {
                    MuleMessage request = null;
                    try
                    {
                        request = muleContext.getClient().request("vm://output?connector=vm-connector-small", 100);
                    }
                    catch (MuleException e)
                    {
                        return false;
                    }
                    return request != null;
                }

                public String describeFailure()
                {
                    return "Some messages didn't arrive";
                }
            });
            System.out.println("Sending message: " + i);
        }
    }
}
