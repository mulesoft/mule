/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.jbpm;

import static org.junit.Assert.assertNotNull;
import org.mule.api.MuleMessage;
import org.mule.module.bpm.BPMS;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;

import org.jbpm.api.ProcessInstance;
import org.junit.Test;

public class ForkedProcessComponentTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "jbpm-component-functional-test-service.xml";
    }

    @Test
    public void testForkedProcess() throws Exception
    {
        BPMS bpms = muleContext.getRegistry().lookupObject(BPMS.class);
        assertNotNull(bpms);

        // Create a new process.
        final MuleMessage response = muleContext.getClient().send("vm://fork", "data", null);
        String processId = ((ProcessInstance) response.getPayload()).getId();

        // The process should be waiting for asynchronous responses from both services
        assertProcessState(bpms, processId, "waitForResponseA / waitForResponseB");

        // ServiceA is initially stopped, so we're still waiting for a response from ServiceA
        assertProcessState(bpms, processId, "waitForResponseA");

        // Start ServiceA
        muleContext.getRegistry().lookupService("ServiceA").resume();

        // The process should have ended.
        assertProcessState(bpms, processId, "ended");
    }

    private void assertProcessState(final BPMS bpms, final String processId, final String state)
    {
        Prober prober = new PollingProber(RECEIVE_TIMEOUT, 50);
        prober.check(new Probe()
        {
            private String processState;

            @Override
            public boolean isSatisfied()
            {
                try
                {
                    ProcessInstance process = (ProcessInstance) bpms.lookupProcess(processId);
                    processState = (String) bpms.getState(process);
                }
                catch (Exception e)
                {
                    return false;
                }

                return state.equals(processState);
            }

            @Override
            public String describeFailure()
            {
                return "process was in state: " + processState + " but expected to contain: " + state;
            }
        });
    }

}
