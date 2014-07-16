/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.soap.axis;

public abstract class AbstractSoapResourceEndpointFunctionalTestCase extends AbstractSoapFunctionalTestCase
{
    @Override
    public String getConfigFile()
    {
        return "axis-" + getTransportProtocol() + "-mule-config.xml";
    }

    protected abstract String getTransportProtocol();

    protected abstract String getSoapProvider();

    @Override
    protected String getRequestResponseEndpoint()
    {
        return getSoapProvider() + ":" + getTransportProtocol() + "://mycomponent?method=echo";
    }

    @Override
    protected String getReceiveEndpoint()
    {
        return getSoapProvider() + ":" + getTransportProtocol() + "://mycomponent2?method=getDate";
    }

    @Override
    protected String getReceiveComplexEndpoint()
    {
        return getSoapProvider() + ":" + getTransportProtocol()
               + "://mycomponent3?method=getPerson&param=Fred";
    }

    @Override
    protected String getSendReceiveComplexEndpoint1()
    {
        return getSoapProvider() + ":" + getTransportProtocol() + "://mycomponent3?method=addPerson";
    }

    @Override
    protected String getSendReceiveComplexEndpoint2()
    {
        return getSoapProvider() + ":" + getTransportProtocol()
               + "://mycomponent3?method=getPerson&param=Dino";
    }

    @Override
    protected String getReceiveComplexCollectionEndpoint()
    {
        return getSoapProvider() + ":" + getTransportProtocol() + "://mycomponent3?method=getPeople";
    }

    @Override
    protected String getDispatchAsyncComplexEndpoint1()
    {
        return getSoapProvider() + ":" + getTransportProtocol() + "://mycomponent3?method=addPerson";
    }

    @Override
    protected String getDispatchAsyncComplexEndpoint2()
    {
        return getSoapProvider() + ":" + getTransportProtocol()
               + "://mycomponent3?method=getPerson&param=Betty";
    }

    @Override
    protected String getTestExceptionEndpoint()
    {
        return getSoapProvider() + ":" + getTransportProtocol() + "://mycomponent3?method=addPerson";
    }

    @Override
    protected String getWsdlEndpoint()
    {
        return getSoapProvider() + ":" + getTransportProtocol() + "://mycomponent?wsdl";
    }

    @Override
    public void testLocationUrlInWSDL() throws Exception
    {
        // Only works with socket based protocols
    }

}
