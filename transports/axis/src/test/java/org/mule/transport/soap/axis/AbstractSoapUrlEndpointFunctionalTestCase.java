/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.soap.axis;


public abstract class AbstractSoapUrlEndpointFunctionalTestCase extends AbstractSoapFunctionalTestCase
{
    protected abstract String getTransportProtocol();

    protected abstract String getSoapProvider();

    protected String getComponentWithoutInterfacesEndpoint()
    {
        return getSoapProvider() + ":" + getTransportProtocol() + "://localhost:" + getPorts().get(2) + "/mule/test";
    }

    @Override
    protected String getRequestResponseEndpoint()
    {
        return getSoapProvider() + ":" + getTransportProtocol()
               + "://localhost:" + getPorts().get(0) + "/mule/mycomponent?method=echo";
    }

    @Override
    protected String getReceiveEndpoint()
    {
        return getSoapProvider() + ":" + getTransportProtocol()
               + "://localhost:" + getPorts().get(1) + "/mule/services/mycomponent2?method=getDate";
    }

    @Override
    protected String getReceiveComplexEndpoint()
    {
        return getSoapProvider() + ":" + getTransportProtocol()
               + "://localhost:" + getPorts().get(1) + "/mycomponent3?method=getPerson&param=Fred";
    }

    @Override
    protected String getSendReceiveComplexEndpoint1()
    {
        return getSoapProvider() + ":" + getTransportProtocol()
               + "://localhost:" + getPorts().get(1) + "/mycomponent3?method=addPerson";
    }

    @Override
    protected String getSendReceiveComplexEndpoint2()
    {
        return getSoapProvider() + ":" + getTransportProtocol()
               + "://localhost:" + getPorts().get(1) + "/mycomponent3?method=getPerson&param=Dino";
    }

    @Override
    protected String getReceiveComplexCollectionEndpoint()
    {
        return getSoapProvider() + ":" + getTransportProtocol()
               + "://localhost:" + getPorts().get(1) + "/mycomponent3?method=getPeople";
    }

    @Override
    protected String getDispatchAsyncComplexEndpoint1()
    {
        return getSoapProvider() + ":" + getTransportProtocol()
               + "://localhost:" + getPorts().get(1) + "/mycomponent3?method=addPerson";
    }

    @Override
    protected String getDispatchAsyncComplexEndpoint2()
    {
        return getSoapProvider() + ":" + getTransportProtocol()
               + "://localhost:" + getPorts().get(1) + "/mycomponent3?method=getPerson&param=Betty";
    }

    @Override
    protected String getTestExceptionEndpoint()
    {
        return getSoapProvider() + ":" + getTransportProtocol()
               + "://localhost:" + getPorts().get(1) + "/mycomponent3?method=addPerson";
    }

    @Override
    protected String getWsdlEndpoint()
    {
        return getTransportProtocol() + "://localhost:" + getPorts().get(0) + "/mule/mycomponent?wsdl";
    }

}
