/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.axis;

public class AxisConnectorJmsTopicsFunctionalTestCase extends AxisConnectorJmsFunctionalTestCase
{
    protected String getRequestResponseEndpoint()
    {
        return getSoapProvider() + ":" + getTransportProtocol() + "://mycomponent?method=echo&resourceInfo=topic";
    }

    protected String getReceiveEndpoint()
    {
        return getSoapProvider() + ":" + getTransportProtocol() + "://mycomponent2?method=getDate&resourceInfo=topic";
    }

    protected String getReceiveComplexEndpoint()
    {
        return getSoapProvider() + ":" + getTransportProtocol()
               + "://mycomponent3?method=getPerson&param=Fred&resourceInfo=topic";
    }

    protected String getSendReceiveComplexEndpoint1()
    {
        return getSoapProvider() + ":" + getTransportProtocol() + "://mycomponent3?method=addPerson&resourceInfo=topic";
    }

    protected String getSendReceiveComplexEndpoint2()
    {
        return getSoapProvider() + ":" + getTransportProtocol()
               + "://mycomponent3?method=getPerson&param=Dino&resourceInfo=topic&resourceInfo=topic";
    }

    protected String getReceiveComplexCollectionEndpoint()
    {
        return getSoapProvider() + ":" + getTransportProtocol() + "://mycomponent3?method=getPeople&resourceInfo=topic";
    }

    protected String getDispatchAsyncComplexEndpoint1()
    {
        return getSoapProvider() + ":" + getTransportProtocol() + "://mycomponent3?method=addPerson&resourceInfo=topic";
    }

    protected String getDispatchAsyncComplexEndpoint2()
    {
        return getSoapProvider() + ":" + getTransportProtocol()
               + "://mycomponent3?method=getPerson&param=Betty&resourceInfo=topic";
    }

    protected String getTestExceptionEndpoint()
    {
        return getSoapProvider() + ":" + getTransportProtocol() + "://mycomponent3?method=addPerson&resourceInfo=topic";
    }

    protected String getWsdlEndpoint()
    {
        return getSoapProvider() + ":" + getTransportProtocol() + "://mycomponent?wsdl";
    }
    
    public String getConfigResources()
    {
        return "axis-jms-topics-mule-config.xml";
    }
}
