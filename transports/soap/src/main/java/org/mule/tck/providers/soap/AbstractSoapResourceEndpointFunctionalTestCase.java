/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.providers.soap;

public abstract class AbstractSoapResourceEndpointFunctionalTestCase extends AbstractSoapFunctionalTestCase
{

    public String getConfigResources()
    {
        return "axis-" + getTransportProtocol() + "-mule-config.xml";
    }

    protected abstract String getTransportProtocol();

    protected abstract String getSoapProvider();

    protected String getRequestResponseEndpoint()
    {
        return getSoapProvider() + ":" + getTransportProtocol() + "://mycomponent?method=echo";
    }

    protected String getReceiveEndpoint()
    {
        return getSoapProvider() + ":" + getTransportProtocol() + "://mycomponent2?method=getDate";
    }

    protected String getReceiveComplexEndpoint()
    {
        return getSoapProvider() + ":" + getTransportProtocol()
               + "://mycomponent3?method=getPerson&param=Fred";
    }

    protected String getSendReceiveComplexEndpoint1()
    {
        return getSoapProvider() + ":" + getTransportProtocol() + "://mycomponent3?method=addPerson";
    }

    protected String getSendReceiveComplexEndpoint2()
    {
        return getSoapProvider() + ":" + getTransportProtocol()
               + "://mycomponent3?method=getPerson&param=Dino";
    }

    protected String getReceiveComplexCollectionEndpoint()
    {
        return getSoapProvider() + ":" + getTransportProtocol() + "://mycomponent3?method=getPeople";
    }

    protected String getDispatchAsyncComplexEndpoint1()
    {
        return getSoapProvider() + ":" + getTransportProtocol() + "://mycomponent3?method=addPerson";
    }

    protected String getDispatchAsyncComplexEndpoint2()
    {
        return getSoapProvider() + ":" + getTransportProtocol()
               + "://mycomponent3?method=getPerson&param=Betty";
    }

    protected String getTestExceptionEndpoint()
    {
        return getSoapProvider() + ":" + getTransportProtocol() + "://mycomponent3?method=addPerson";
    }

    protected String getWsdlEndpoint()
    {
        return getSoapProvider() + ":" + getTransportProtocol() + "://mycomponent?wsdl";
    }

    public void testLocationUrlInWSDL() throws Exception
    {
        // Only works with socket based protocols
    }

}
