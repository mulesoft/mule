/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.jbpm;

import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests the jBPM wrapper with a simple process.
 */
public class JbpmUnitTestCase extends AbstractMuleTestCase
{
    @Test
    public void testDeployAndRun() throws Exception 
    {
        Jbpm jbpm = new Jbpm();
        jbpm.initialise();

        // Deploy the process
        jbpm.deployProcess("simple-process.jpdl.xml");

        // Start the process
        Object process = jbpm.startProcess("simple", null, null);
        assertNotNull(process);
        Object processId = jbpm.getId(process);
        
        // The process should be started and in a wait state.
        process = jbpm.lookupProcess(processId);
        assertNotNull(process);             
        assertEquals("dummyState", jbpm.getState(process));

        // Advance the process one step.
        process = jbpm.advanceProcess(processId);

        // The process should have ended.
        assertNotNull(process);             
        assertTrue(jbpm.hasEnded(process));

        jbpm.dispose();
    }
}
