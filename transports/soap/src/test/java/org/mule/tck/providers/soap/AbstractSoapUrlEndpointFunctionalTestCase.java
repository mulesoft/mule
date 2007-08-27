/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.providers.soap;

public abstract class AbstractSoapUrlEndpointFunctionalTestCase extends AbstractSoapFunctionalTestCase
{
    protected abstract String getTransportProtocol();

    protected abstract String getSoapProvider();

    protected String getComponentWithoutInterfacesEndpoint()
    {
        return getSoapProvider() + ":" + getTransportProtocol() + "://localhost:62111/mule/test";
    }

    protected String getRequestResponseEndpoint()
    {
        return getSoapProvider() + ":" + getTransportProtocol()
               + "://localhost:62108/mule/mycomponent?method=echo";
    }

    protected String getReceiveEndpoint()
    {
        return getSoapProvider() + ":" + getTransportProtocol()
               + "://localhost:62109/mule/services/mycomponent2?method=getDate";
    }

    protected String getReceiveComplexEndpoint()
    {
        return getSoapProvider() + ":" + getTransportProtocol()
               + "://localhost:62109/mycomponent3?method=getPerson&param=Fred";
    }

    protected String getSendReceiveComplexEndpoint1()
    {
        return getSoapProvider() + ":" + getTransportProtocol()
               + "://localhost:62109/mycomponent3?method=addPerson";
    }

    protected String getSendReceiveComplexEndpoint2()
    {
        return getSoapProvider() + ":" + getTransportProtocol()
               + "://localhost:62109/mycomponent3?method=getPerson&param=Dino";
    }

    protected String getReceiveComplexCollectionEndpoint()
    {
        return getSoapProvider() + ":" + getTransportProtocol()
               + "://localhost:62109/mycomponent3?method=getPeople";
    }

    protected String getDispatchAsyncComplexEndpoint1()
    {
        return getSoapProvider() + ":" + getTransportProtocol()
               + "://localhost:62109/mycomponent3?method=addPerson";
    }

    protected String getDispatchAsyncComplexEndpoint2()
    {
        return getSoapProvider() + ":" + getTransportProtocol()
               + "://localhost:62109/mycomponent3?method=getPerson&param=Betty";
    }

    protected String getTestExceptionEndpoint()
    {
        return getSoapProvider() + ":" + getTransportProtocol()
               + "://localhost:62109/mycomponent3?method=addPerson";
    }

    protected String getWsdlEndpoint()
    {
        return getTransportProtocol() + "://localhost:62108/mule/mycomponent?wsdl";
    }

}
