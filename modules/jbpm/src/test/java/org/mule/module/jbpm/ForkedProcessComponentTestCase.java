/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.jbpm;

import org.mule.api.MuleMessage;
import org.mule.module.bpm.BPMS;
import org.mule.tck.junit4.FunctionalTestCase;

import org.jbpm.api.ProcessInstance;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ForkedProcessComponentTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "jbpm-component-functional-test-service.xml";
    }

    @Test
    public void testForkedProcess() throws Exception 
    {
        BPMS bpms = muleContext.getRegistry().lookupObject(BPMS.class);
        assertNotNull(bpms);

        // Create a new process.
        MuleMessage response = muleContext.getClient().send("vm://fork", "data", null);                      
        ProcessInstance process = (ProcessInstance) response.getPayload();
        
        // The process should be waiting for asynchronous responses from both services
        String state = (String) bpms.getState(process);
        assertTrue(state.contains("waitForResponseA"));
        assertTrue(state.contains("waitForResponseB"));

        Thread.sleep(2000);

        // ServiceA is initially stopped, so we're still waiting for a response from ServiceA
        process = (ProcessInstance) bpms.lookupProcess(process.getId());
        assertEquals("waitForResponseA", bpms.getState(process));

        // Start ServiceA
        muleContext.getRegistry().lookupService("ServiceA").resume();
        Thread.sleep(2000);
                    
        // The process should have ended.
        process = (ProcessInstance) bpms.lookupProcess(process.getId());
        assertTrue("Process should have ended, but is in state " + bpms.getState(process), 
                bpms.hasEnded(process));
    }

}
