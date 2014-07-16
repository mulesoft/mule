/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.soap.axis;

public class AxisConnectorJmsTopicsFunctionalTestCase extends AxisConnectorJmsFunctionalTestCase
{
    @Override
    public String getConfigFile()
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
