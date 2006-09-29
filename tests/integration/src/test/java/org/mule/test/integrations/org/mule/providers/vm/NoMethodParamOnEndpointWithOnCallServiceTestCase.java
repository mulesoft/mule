/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the BSD style
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integrations.org.mule.providers.vm;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.test.integration.service.Person;
import org.mule.test.integration.service.TestServiceComponent;
import org.mule.umo.UMOMessage;

public class NoMethodParamOnEndpointWithOnCallServiceTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/test/integrations/org/mule/providers/vm/noMethodOnEndpoint-vm-test-config.xml";
    }

    /*
     * Since the Echo Component in the TestServiceComponent implements the Callable
     * interface, the TooManySatisfiable error does not arise. However the same
     * problem exists as with the pojo, that is, the required method is not called.
     * These tests will demonstrate that unless the MULE_SERVICE_METHOD is set on the
     * endpoint, then the call to the required method will fail. The method passed in
     * the vm is being ignored.
     */

    public void testFailingCallEchoComponentWithMethod() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage message = client.send("vm://service?method=echo", "test", null);
        assertNotNull(message.getPayload());
        assertTrue(message.getPayload() instanceof String);
        assertFalse(message.getPayloadAsString().equals("test"));
    }

    public void testFailingCallGetPersonComponentWithMethod() throws Exception
    {
        new TestServiceComponent();
        MuleClient client = new MuleClient();
        UMOMessage message = client.send("vm://service?method=getPerson", "Wilma", null);
        assertNotNull(message.getPayload());
        assertFalse(message.getPayload() instanceof Person);
        assertFalse(message.getPayloadAsString().equals("Wilma Flintstone"));
    }

    public void testFailingCallEchoComponentNoMethod() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage message = client.send("vm://service", "test", null);
        assertNotNull(message.getPayload());
        assertTrue(message.getPayload() instanceof String);
        assertFalse(message.getPayloadAsString().equals("test"));
    }

    public void testFailingCallGetPersonComponentNoMethod() throws Exception
    {
        new TestServiceComponent();
        MuleClient client = new MuleClient();
        UMOMessage message = client.send("vm://service", "Wilma", null);
        assertNotNull(message.getPayload());
        assertFalse(message.getPayload() instanceof Person);
        assertFalse(message.getPayloadAsString().equals("Wilma Flintstone"));
    }
}
