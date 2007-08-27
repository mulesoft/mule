/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.xfire;

import org.mule.extras.client.MuleClient;
import org.mule.impl.MuleMessage;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.testmodels.services.Person;
import org.mule.tck.testmodels.services.PersonResponse;
import org.mule.umo.UMOMessage;

public class ComplexTypeMethodTestCase extends FunctionalTestCase
{
    public void testSendComplexType() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage result = client.send("xfireEndpoint", new MuleMessage(new Person("Jane", "Doe")));
        assertNotNull(result.getPayload());
        assertTrue(result.getPayload() instanceof PersonResponse);
        assertTrue(((PersonResponse)result.getPayload()).getPerson().getFirstName().equalsIgnoreCase("Jane"));
        // call this just to be sure it doesn't throw an exception
        ((PersonResponse)result.getPayload()).getTime();
    }

    public void testSendComplexTypeUsingWSDLXfire() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage result = client.send("wsdlEndpoint", new MuleMessage(new Person("Jane", "Doe")));
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