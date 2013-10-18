/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
