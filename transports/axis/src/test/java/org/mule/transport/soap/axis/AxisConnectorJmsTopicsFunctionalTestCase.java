/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.soap.axis;

public class AxisConnectorJmsTopicsFunctionalTestCase extends AxisConnectorJmsFunctionalTestCase
{

    @Override
    public String getConfigResources()
    {
        return "axis-jms-topics-mule-config.xml";
    }

    @Override
    protected String getRequestResponseEndpoint()
    {
        return getSoapProvider() + ":" + getTransportProtocol() + "://mycomponent?method=echo&resourceInfo=topic";
    }

    @Override
    protected String getReceiveEndpoint()
    {
        return getSoapProvider() + ":" + getTransportProtocol() + "://mycomponent2?method=getDate&resourceInfo=topic";
    }

    @Override
    protected String getReceiveComplexEndpoint()
    {
        return getSoapProvider() + ":" + getTransportProtocol()
               + "://mycomponent3?method=getPerson&param=Fred&resourceInfo=topic";
    }

    @Override
    protected String getSendReceiveComplexEndpoint1()
    {
        return getSoapProvider() + ":" + getTransportProtocol() + "://mycomponent3?method=addPerson&resourceInfo=topic";
    }

    @Override
    protected String getSendReceiveComplexEndpoint2()
    {
        return getSoapProvider() + ":" + getTransportProtocol()
               + "://mycomponent3?method=getPerson&param=Dino&resourceInfo=topic&resourceInfo=topic";
    }

    @Override
    protected String getReceiveComplexCollectionEndpoint()
    {
        return getSoapProvider() + ":" + getTransportProtocol() + "://mycomponent3?method=getPeople&resourceInfo=topic";
    }

    @Override
    protected String getDispatchAsyncComplexEndpoint1()
    {
        return getSoapProvider() + ":" + getTransportProtocol() + "://mycomponent3?method=addPerson&resourceInfo=topic";
    }

    @Override
    protected String getDispatchAsyncComplexEndpoint2()
    {
        return getSoapProvider() + ":" + getTransportProtocol()
               + "://mycomponent3?method=getPerson&param=Betty&resourceInfo=topic";
    }

    @Override
    protected String getTestExceptionEndpoint()
    {
        return getSoapProvider() + ":" + getTransportProtocol() + "://mycomponent3?method=addPerson&resourceInfo=topic";
    }

    @Override
    protected String getWsdlEndpoint()
    {
        return getSoapProvider() + ":" + getTransportProtocol() + "://mycomponent?wsdl";
    }
}
