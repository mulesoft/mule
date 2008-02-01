/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.soap.xfire;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.testmodels.services.Person;
import org.mule.tck.testmodels.services.PersonResponse;

public class ComplexTypeMethodTestCase extends FunctionalTestCase
{
    
    public void testSendComplexType() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage result = client.send("xfireEndpoint", new DefaultMuleMessage(new Person("Jane", "Doe")));
        assertNotNull(result.getPayload());
        assertTrue(result.getPayload() instanceof PersonResponse);
        assertTrue(((PersonResponse)result.getPayload()).getPerson().getFirstName().equalsIgnoreCase("Jane"));
        // call this just to be sure it doesn't throw an exception
        ((PersonResponse)result.getPayload()).getTime();
    }

    protected String getConfigResources()
    {
        return "xfire-complex-type-conf.xml";
    }

}