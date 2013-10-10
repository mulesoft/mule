/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.usecases.axis.clientbridge;

import org.mule.api.MuleException;
import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.context.MuleContextFactory;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.module.client.MuleClient;

public class Client
{
    private static final String LOCAL_ENDPOINT = "vm://complexRequest";
    private static final String AXIS_ENDPOINT = "axis:http://localhost:8002/axisService/doSomeWork";
    protected static MuleContext muleContext;

    public static void main(String[] args) throws Exception
    {
        muleContext = null;

        try
        {
            SpringXmlConfigurationBuilder builder = new SpringXmlConfigurationBuilder("clientbridge/conf/client-mule-config.xml");
            MuleContextFactory muleContextFactory = new DefaultMuleContextFactory();
            muleContext = muleContextFactory.createMuleContext(builder);

            Client c = new Client();
            c.execute();
        }
        finally
        {
            if (muleContext != null)
            {
               muleContext.dispose();
            }
        }
    }

    private MuleClient client;

    private void execute() throws MuleException
    {
        client = new MuleClient(muleContext);

        try
        {
            executeComplexity();
            complexRequest();
        }
        finally
        {
            if (client != null)
            {
                client.dispose();
            }
        }
    }

    private void executeComplexity() throws MuleException
    {
        System.err.println("\nexecuteComplexity");
        Object result = client.send(AXIS_ENDPOINT + "?method=executeComplexity", new ComplexData("Foo",
            new Integer(42)), null);
        System.err.println(result);
        MuleMessage message = (MuleMessage)result;
        ComplexData data = (ComplexData)message.getPayload();
        System.err.println(data);
    }

    private void complexRequest() throws MuleException
    {
        System.err.println("\ncomplexRequest");
        Object result = client.send(LOCAL_ENDPOINT, new ComplexData("Foo", new Integer(84)), null);
        System.err.println(result);
        MuleMessage message = (MuleMessage)result;
        ComplexData data = (ComplexData)message.getPayload();
        System.err.println(data);
    }
}
