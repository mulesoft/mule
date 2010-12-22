/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.activiti;

import org.mule.api.MuleMessage;
import org.mule.module.activiti.action.Operation;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

import java.util.Map;

public class ActivitiFunctionalTestCase extends FunctionalTestCase
{

    public void testFunctional() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        
        //wait for the process creation
        MuleMessage messageProcessCreated = client.request("vm://processCreated", 10000);
        String processDefinitionId = messageProcessCreated.getPayloadAsString();
        assertEquals("financialReport:1", processDefinitionId);
        
        //wait for the task to be claimed
        MuleMessage messageClaimedTask = client.request("vm://claimedTask", 10000);
        Map selectedTask = (Map) messageClaimedTask.getPayload();
        assertFalse(selectedTask.isEmpty());
        assertNotNull(selectedTask.get("taskId"));
        assertEquals(Operation.CLAIM, selectedTask.get("operation"));
    }

    @Override
    protected String getConfigResources()
    {
        return "activiti-process-and-tasks.xml";
    }
}
