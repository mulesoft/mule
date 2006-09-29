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

public class MethodParamOnEndpointWithOnCallServiceTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/test/integrations/org/mule/providers/vm/methodOnEndpoint-vm-test-config.xml";
    }

    /*
     * Since the Echo Component in the TestServiceComponent implements the Callable
     * interface, the TooManySatisfiable error does not arise. However the same
     * problem exists as with the pojo, that is, the required method is not called.
     * These tests will demonstrate that with the MULE_SERVICE_METHOD set on the
     * endpoint, then the call to the required method will succeed. The property
     * MULE_SERVICE_METHOD must be placed in the url otherwise, if the format
     * vm://service?method=myMethod is used, the method call could fail.
     */

    public void testCallEchoComponentWithMuleProperty() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage message = client.send("vm://service?MULE_SERVICE_METHOD=echo", "test", null);
        assertNotNull(message.getPayload());
        assertTrue(message.getPayload() instanceof String);
        assertEquals(message.getPayloadAsString(), "test");
    }

    public void testCallGetPersonComponentWithMuleProperty() throws Exception
    {
        new TestServiceComponent();
        MuleClient client = new MuleClient();
        UMOMessage message = client.send("vm://service?MULE_SERVICE_METHOD=getPerson", "Wilma",
            null);
        assertNotNull(message.getPayload());
        assertTrue(message.getPayload() instanceof Person);
        assertEquals(message.getPayloadAsString(), "Wilma Flintstone");
    }

    public void testCallEchoComponentWithoutMulePropertyName() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage message = client.send("vm://service?method=echo", "test", null);
        assertNull(message);
    }
}
