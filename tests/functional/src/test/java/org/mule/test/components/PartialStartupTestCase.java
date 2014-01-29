/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.components;

import static org.junit.Assert.fail;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;

import org.junit.Test;

public class PartialStartupTestCase extends FunctionalTestCase
{

    public PartialStartupTestCase()
    {
        super();
        setStartContext(false);
    }

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/components/partial-startup-test.xml";
    }

    @Test
    public void testStopAfterPartialStartup() throws Exception
    {
        try
        {
            muleContext.start();
            fail("Expected Mule to fail to start, due to our RudeMessageProcessor");
        }
        catch(Exception e)
        {
            System.err.println("Expected Exception:");
            e.printStackTrace();
        }

        //Mule failed to start, so go ahead and dispose it(Mule will not let us call stop at this point)
        muleContext.dispose();


        new PollingProber(10000, 100).check(new Probe()
        {
            @Override
            public boolean isSatisfied()
            {
                boolean threadsStopped = true;
                //Make sure that there are no Mule threads running
                for(Thread t : Thread.getAllStackTraces().keySet())
                {
                    if(t.getName().startsWith("SHUTDOWN_TEST_FLOW") || t.getName().startsWith("MuleServer"))
                    {
                        threadsStopped = false;
                        break;
                    }
                }
                return threadsStopped;
            }

            @Override
            public String describeFailure()
            {
                return "mule threads running during dispose";
            }
        });
    }
}
