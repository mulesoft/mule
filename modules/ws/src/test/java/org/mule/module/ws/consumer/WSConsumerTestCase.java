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
import org.mule.util.ClassUtils;

import org.junit.Test;

@SmallTest
public class WSConsumerTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void initialisesCorrectlyWithValidArguments() throws MuleException
    {
        WSConsumer wsConsumer = createConsumer();
        wsConsumer.initialise();
    }

    @Test(expected = InitialisationException.class)
    public void failsToInitializeWithInvalidWsdlLocation() throws MuleException
    {
        WSConsumer wsConsumer = createConsumer();
        wsConsumer.getConfig().setWsdlLocation("invalid");
        wsConsumer.initialise();
    }

    @Test(expected = InitialisationException.class)
    public void failsToInitializeWithInvalidService() throws MuleException
    {
        WSConsumer wsConsumer = createConsumer();
        wsConsumer.getConfig().setService("invalid");
        wsConsumer.initialise();
    }

    @Test(expected = InitialisationException.class)
    public void failsToInitializeWithInvalidPort() throws MuleException
    {
        WSConsumer wsConsumer = createConsumer();
        wsConsumer.getConfig().setPort("invalid");
        wsConsumer.initialise();
    }

    @Test(expected = InitialisationException.class)
    public void failsToInitializeWithInvalidOperation() throws MuleException
    {
        WSConsumer wsConsumer = createConsumer();
        wsConsumer.setOperation("invalid");
        wsConsumer.initialise();
    }


    private WSConsumer createConsumer()
    {
        WSConsumerConfig wsConsumerConfig = new WSConsumerConfig();

        wsConsumerConfig.setWsdlLocation(ClassUtils.getClassPathRoot(WSConsumerTestCase.class).getPath() + "Echo.wsdl");
        wsConsumerConfig.setServiceAddress("http://localhost/echo");
        wsConsumerConfig.setService("EchoService");
        wsConsumerConfig.setPort("EchoPort");
        wsConsumerConfig.setMuleContext(muleContext);

        WSConsumer wsConsumer = new WSConsumer();
        wsConsumer.setOperation("echo");
        wsConsumer.setConfig(wsConsumerConfig);
        wsConsumer.setMuleContext(muleContext);

        return wsConsumer;
    }
}
