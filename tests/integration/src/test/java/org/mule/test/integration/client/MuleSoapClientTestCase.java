/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.test.integration.client;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.test.integration.service.Person;
import org.mule.umo.UMOMessage;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class MuleSoapClientTestCase extends FunctionalTestCase
{
    public MuleSoapClientTestCase() {
        setDisposeManagerPerSuite(true);
    }

    protected String getConfigResources() {
        return "org/mule/test/integration/client/glue-test-mule-config.xml";
    }

    public void testRequestResponse() throws Throwable
    {
        MuleClient client = new MuleClient();

        UMOMessage result = client.send("glue:http://localhost:38004/mule/mycomponent2?method=echo", "test", null);
        assertNotNull(result);
        assertEquals("test", result.getPayloadAsString());
    }

    public void testRequestResponseComplex() throws Exception
    {
        MuleClient client = new MuleClient();

        UMOMessage result = client.send("glue:http://localhost:38004/mule/mycomponent3?method=getPerson", "Fred", null);
        assertNotNull(result);
        System.out.println(result.getPayload());
        assertTrue(result.getPayload() instanceof Person);
        assertEquals("Fred", ((Person) result.getPayload()).getFirstName());
        assertEquals("Flintstone", ((Person) result.getPayload()).getLastName());
    }

    public void testRequestResponseComplex2() throws Exception
    {
        MuleClient client = new MuleClient();

        String[] args = new String[] { "Betty", "Rubble" };
        UMOMessage result = client.send("glue:http://localhost:38004/mule/mycomponent3?method=addPerson", args, null);
        assertNotNull(result);
        assertTrue(result.getPayload() instanceof Person);
        assertEquals("Betty", ((Person) result.getPayload()).getFirstName());
        assertEquals("Rubble", ((Person) result.getPayload()).getLastName());

        // do a receive
        result = client.send("glue:http://localhost:38004/mule/mycomponent3?method=getPerson", "Betty", null);
        assertNotNull(result);
        assertTrue(result.getPayload() instanceof Person);
        assertEquals("Betty", ((Person) result.getPayload()).getFirstName());
        assertEquals("Rubble", ((Person) result.getPayload()).getLastName());

    }
}
