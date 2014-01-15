/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.consumer;


import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.transport.http.HttpConnector;
import org.mule.util.ClassUtils;

import org.junit.Test;

@SmallTest
public class WSConsumerTestCase extends AbstractMuleContextTestCase
{

    private String wsdlLocation = ClassUtils.getClassPathRoot(getClass()).getPath() + "Echo.wsdl";
    private String wsdlService = "EchoService";
    private String wsdlPort = "EchoPort";
    private String wsdlOperation = "echo";
    private String serviceAddress = "http://localhost/echo";

    @Test
    public void initialisesCorrectlyWithValidArguments() throws MuleException
    {
        WSConsumer wsConsumer = new WSConsumer(wsdlLocation, wsdlService, wsdlPort, wsdlOperation, serviceAddress, null, null, muleContext);
        wsConsumer.initialise();
    }

    @Test(expected = InitialisationException.class)
    public void failsToInitializeWithInvalidWsdlLocation() throws MuleException
    {
        WSConsumer wsConsumer = new WSConsumer("invalid.wsdl", wsdlService, wsdlPort, wsdlOperation, serviceAddress, null, null, muleContext);
        wsConsumer.initialise();
    }

    @Test(expected = InitialisationException.class)
    public void failsToInitializeWithInvalidWsdlService() throws MuleException
    {
        WSConsumer wsConsumer = new WSConsumer(wsdlLocation, "invalidService", wsdlPort, wsdlOperation, serviceAddress, null, null, muleContext);
        wsConsumer.initialise();
    }

    @Test(expected = InitialisationException.class)
    public void failsToInitializeWithInvalidWsdlPort() throws MuleException
    {
        WSConsumer wsConsumer = new WSConsumer(wsdlLocation, wsdlService, "invalidPort", wsdlOperation, serviceAddress, null, null, muleContext);
        wsConsumer.initialise();
    }

    @Test(expected = InitialisationException.class)
    public void failsToInitializeWithInvalidWsdlOperation() throws MuleException
    {
        WSConsumer wsConsumer = new WSConsumer(wsdlLocation, wsdlService, wsdlPort, "invalidOperation", serviceAddress, null, null, muleContext);
        wsConsumer.initialise();
    }

    @Test(expected = IllegalStateException.class)
    public void failsToInitializeWhenConnectorDoesNotSupportProtocol() throws MuleException
    {
        HttpConnector httpConnector = new HttpConnector(muleContext);
        new WSConsumer(wsdlLocation, wsdlService, wsdlPort, wsdlOperation, "jms://test", httpConnector, null, muleContext);
    }

}
