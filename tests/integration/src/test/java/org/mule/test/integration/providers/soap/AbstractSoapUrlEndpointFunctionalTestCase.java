/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.test.integration.providers.soap;




/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public abstract class AbstractSoapUrlEndpointFunctionalTestCase extends AbstractSoapFunctionalTestCase
{
    protected abstract String getTransportProtocol();

    protected abstract String getSoapProvider();


    protected String getComponentWithoutInterfacesEndpoint() {
        return getSoapProvider() + ":" + getTransportProtocol() + "://localhost:38011/mule/test";
    }
    protected String getRequestResponseEndpoint() {
        return getSoapProvider() + ":" + getTransportProtocol() + "://localhost:38008/mule/mycomponent?method=echo";
    }

    protected String getReceiveEndpoint() {
        return getSoapProvider() + ":" + getTransportProtocol() + "://localhost:38009/mule/services/mycomponent2?method=getDate";
    }

    protected String getReceiveComplexEndpoint() {
        return getSoapProvider() + ":" + getTransportProtocol() + "://localhost:38009/mycomponent3?method=getPerson&param=Fred";
    }

    protected String getSendReceiveComplexEndpoint1() {
        return getSoapProvider() + ":" + getTransportProtocol() + "://localhost:38009/mycomponent3?method=addPerson";
    }

    protected String getSendReceiveComplexEndpoint2() {
        return getSoapProvider() + ":" + getTransportProtocol() + "://localhost:38009/mycomponent3?method=getPerson&param=Dino";
    }

    protected String getReceiveComplexCollectionEndpoint() {
        return getSoapProvider() + ":" + getTransportProtocol() + "://localhost:38009/mycomponent3?method=getPeople";
    }

    protected String getDispatchAsyncComplexEndpoint1() {
        return getSoapProvider() + ":" + getTransportProtocol() + "://localhost:38010/mycomponent4?method=addPerson";
    }

    protected String getDispatchAsyncComplexEndpoint2() {
        return getSoapProvider() + ":" + getTransportProtocol() + "://localhost:38009/mycomponent3?method=getPerson&param=Betty";
    }

    protected String getTestExceptionEndpoint() {
        return getSoapProvider() + ":" + getTransportProtocol() + "://localhost:38009/mycomponent3?method=addPerson";
    }

    protected String getWsdlEndpoint() {
        return getTransportProtocol() + "://localhost:38008/mule/mycomponent?wsdl";
    }
}
