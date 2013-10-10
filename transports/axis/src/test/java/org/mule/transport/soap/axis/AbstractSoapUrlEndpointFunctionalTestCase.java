/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.soap.axis;


import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;

public abstract class AbstractSoapUrlEndpointFunctionalTestCase extends AbstractSoapFunctionalTestCase
{

    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");

    @Rule
    public DynamicPort dynamicPort2 = new DynamicPort("port2");

    @Rule
    public DynamicPort dynamicPort3 = new DynamicPort("port3");

    protected abstract String getTransportProtocol();

    protected abstract String getSoapProvider();

    protected String getComponentWithoutInterfacesEndpoint()
    {
        return getSoapProvider() + ":" + getTransportProtocol() + "://localhost:" + dynamicPort3.getNumber() + "/mule/test";
    }

    @Override
    protected String getRequestResponseEndpoint()
    {
        return getSoapProvider() + ":" + getTransportProtocol()
               + "://localhost:" + dynamicPort1.getNumber() + "/mule/mycomponent?method=echo";
    }

    @Override
    protected String getReceiveEndpoint()
    {
        return getSoapProvider() + ":" + getTransportProtocol()
               + "://localhost:" + dynamicPort2.getNumber() + "/mule/services/mycomponent2?method=getDate";
    }

    @Override
    protected String getReceiveComplexEndpoint()
    {
        return getSoapProvider() + ":" + getTransportProtocol()
               + "://localhost:" + dynamicPort2.getNumber() + "/mycomponent3?method=getPerson&param=Fred";
    }

    @Override
    protected String getSendReceiveComplexEndpoint1()
    {
        return getSoapProvider() + ":" + getTransportProtocol()
               + "://localhost:" + dynamicPort2.getNumber() + "/mycomponent3?method=addPerson";
    }

    @Override
    protected String getSendReceiveComplexEndpoint2()
    {
        return getSoapProvider() + ":" + getTransportProtocol()
               + "://localhost:" + dynamicPort2.getNumber() + "/mycomponent3?method=getPerson&param=Dino";
    }

    @Override
    protected String getReceiveComplexCollectionEndpoint()
    {
        return getSoapProvider() + ":" + getTransportProtocol()
               + "://localhost:" + dynamicPort2.getNumber() + "/mycomponent3?method=getPeople";
    }

    @Override
    protected String getDispatchAsyncComplexEndpoint1()
    {
        return getSoapProvider() + ":" + getTransportProtocol()
               + "://localhost:" + dynamicPort2.getNumber() + "/mycomponent3?method=addPerson";
    }

    @Override
    protected String getDispatchAsyncComplexEndpoint2()
    {
        return getSoapProvider() + ":" + getTransportProtocol()
               + "://localhost:" + dynamicPort2.getNumber() + "/mycomponent3?method=getPerson&param=Betty";
    }

    @Override
    protected String getTestExceptionEndpoint()
    {
        return getSoapProvider() + ":" + getTransportProtocol()
               + "://localhost:" + dynamicPort2.getNumber() + "/mycomponent3?method=addPerson";
    }

    @Override
    protected String getWsdlEndpoint()
    {
        return getTransportProtocol() + "://localhost:" + dynamicPort1.getNumber() + "/mule/mycomponent?wsdl";
    }

}
