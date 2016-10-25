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

import java.util.Arrays;

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
        assertProcessState(new MultipleStringStateProbe(bpms, processId, "waitForResponseA", "waitForResponseB"));

        // Start service B
        muleContext.getRegistry().lookupService("ServiceB").resume();

        // ServiceA is initially stopped, so we're still waiting for a response from ServiceA
        assertProcessState(new EqualStateProbe(bpms, processId, "waitForResponseA"));

        // Start ServiceA
        muleContext.getRegistry().lookupService("ServiceA").resume();

        // The process should have ended.
        assertProcessState(new EqualStateProbe(bpms, processId, "ended"));
    }

    private void assertProcessState(Probe probe)
    {
        Prober prober = new PollingProber(RECEIVE_TIMEOUT, 50);
        prober.check(probe);
    }

    private static class EqualStateProbe implements Probe
    {

        private final BPMS bpms;
        private final String processId;
        private final String state;
        private String processState;

        public EqualStateProbe(BPMS bpms, String processId, String state)
        {
            this.bpms = bpms;
            this.processId = processId;
            this.state = state;
        }

        @Override
        public boolean isSatisfied()
        {
            try
            {
                processState = getProcessState(bpms, processId);
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
    }

    private static  String getProcessState(BPMS bpms, String processId) throws Exception
    {
        ProcessInstance process = (ProcessInstance) bpms.lookupProcess(processId);
        return (String) bpms.getState(process);
    }

    private static class MultipleStringStateProbe implements Probe
    {

        private final BPMS bpms;
        private final String processId;
        private final String[] states;
        private String processState;

        public MultipleStringStateProbe(BPMS bpms, String processId, String... states)
        {
            this.bpms = bpms;
            this.processId = processId;
            this.states = states;
        }

        @Override
        public boolean isSatisfied()
        {
            try
            {
                processState = getProcessState(bpms, processId);
            }
            catch (Exception e)
            {
                return false;
            }

            for (String state : states)
            {
                if (!processState.contains(state))
                {
                    return false;
                }
            }

            return true;
        }

        @Override
        public String describeFailure()
        {
            return "process was in state: " + processState + " but expected to contain: " + Arrays.toString(states);
        }
    }
}
