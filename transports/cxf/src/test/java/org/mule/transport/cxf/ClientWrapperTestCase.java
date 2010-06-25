/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.endpoint.MuleEndpointURI;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.transport.cxf.transport.MuleUniversalConduit;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.Binding;
import org.apache.cxf.binding.BindingFactory;
import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ClientImpl;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.EndpointImpl;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.ChainInitiationObserver;
import org.apache.cxf.transport.ConduitInitiator;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.transport.MessageObserver;

public class ClientWrapperTestCase extends AbstractMuleTestCase
{
    private Bus bus;
    private ImmutableEndpoint immutableEndpoint;
    private ClientWrapper clientWrapper;
    private DestinationFactoryManager destinationFactoryManager;
    private DestinationFactory destinationFactory;
    private Destination destination;
    private ChainInitiationObserver chainInitiationObserver;
    private Endpoint endpoint;
    private EndpointInfo endpointInfo;
    private ConduitInitiatorManager conduitInitiatorManager;
    private ConduitInitiator conduitInitiator;
    private MuleUniversalConduit conduit;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        bus = mock(Bus.class);
        immutableEndpoint = mock(ImmutableEndpoint.class);

        destinationFactoryManager = mock(DestinationFactoryManager.class);
        destinationFactory = mock(DestinationFactory.class);
        destination = mock(Destination.class);
        chainInitiationObserver = mock(ChainInitiationObserver.class);
        endpoint = mock(EndpointImpl.class);
        endpointInfo = mock(EndpointInfo.class);
        conduitInitiatorManager = mock(ConduitInitiatorManager.class);
        conduitInitiator = mock(ConduitInitiator.class);
        conduit = mock(MuleUniversalConduit.class);

        when(immutableEndpoint.getEndpointURI()).thenReturn(
            new MuleEndpointURI("http://some.dummy.endpoint", muleContext));
        when(bus.getExtension(DestinationFactoryManager.class)).thenReturn(destinationFactoryManager);
        when(destinationFactoryManager.getDestinationFactoryForUri(any(String.class))).thenReturn(
            destinationFactory);
        when(destinationFactory.getDestination(any(EndpointInfo.class))).thenReturn(destination);
        when(destination.getMessageObserver()).thenReturn(chainInitiationObserver);
        when(chainInitiationObserver.getEndpoint()).thenReturn(endpoint);
        when(endpoint.getEndpointInfo()).thenReturn(endpointInfo);
        String transportId = "someId";
        when(endpointInfo.getTransportId()).thenReturn(transportId);
        when(bus.getExtension(ConduitInitiatorManager.class)).thenReturn(conduitInitiatorManager);
        when(conduitInitiatorManager.getConduitInitiator(transportId)).thenReturn(conduitInitiator);
        when(conduitInitiator.getConduit(endpointInfo)).thenReturn(conduit);
    }

    private void commonPostExecutionVerifications()
    {
        verify(conduit).setMuleEndpoint(immutableEndpoint);
        verify(conduit).setApplyTransformersToProtocol(true);
    }

    /**
     * This method test the default creation of the {@link ClientWrapper}, which is
     * the one that calls the {@link ClientWrapper#createClientFromLocalServer(Bus)}.
     * 
     * @throws Exception
     */
    public void testCanCreateClientWrapper_FromLocalServer() throws Exception
    {
        clientWrapper = new ClientWrapper(bus, immutableEndpoint);

        assertTrue(clientWrapper.getClient() instanceof ClientImpl);
        ClientImpl clientImpl = (ClientImpl) clientWrapper.getClient();
        assertSame(this.endpoint, clientImpl.getConduitSelector().getEndpoint());
        verify(conduit).setMessageObserver(any(MessageObserver.class));
        commonPostExecutionVerifications();
    }

    /**
     * This method test the creation of a {@link ClientWrapper} using the
     * {@link ClientWrapper#createClientFromClass(Bus, String)} method (the class is
     * provided {@link ImmutableEndpoint#getProperty(Object)
     * ImmutableEndpoint.getProperty(CxfConstants.CLIENT_CLASS)}.
     * 
     * @throws Exception
     */
    public void testCanCreateClientWrapper_FromClientClass() throws Exception
    {
        when(DummyService.client.getEndpoint()).thenReturn(endpoint);
        when(DummyService.client.getConduit()).thenReturn(conduit);
        when(immutableEndpoint.getProperty(CxfConstants.CLIENT_CLASS)).thenReturn(
            DummyService.class.getName());
        when(immutableEndpoint.getProperty(CxfConstants.CLIENT_PORT)).thenReturn("Port");

        clientWrapper = new ClientWrapper(bus, immutableEndpoint);

        assertSame(DummyService.client, clientWrapper.getClient());
        commonPostExecutionVerifications();
    }

    /**
     * This method test the creation of a {@link ClientWrapper} using the
     * {@link ClientWrapper#createClientProxy(Bus)} (this happens when the property
     * <code>proxy</code> is provided in
     * {@link ImmutableEndpoint#getProperty(Object)
     * ImmutableEndpoint.getProperty(CxfConstants.PROXY)}.
     * 
     * @throws Exception
     */
    public void testCanCreateClientWrapper_Proxy() throws Exception
    {
        BindingFactoryManager bindingFactoryManager = mock(BindingFactoryManager.class);
        BindingFactory bindingFactory = mock(BindingFactory.class);
        BindingInfo bindingInfo = mock(BindingInfo.class);
        Binding binding = mock(Binding.class);

        when(immutableEndpoint.getProperty(CxfConstants.PROXY)).thenReturn("true");
        when(bus.getExtension(BindingFactoryManager.class)).thenReturn(bindingFactoryManager);
        when(bindingFactoryManager.getBindingFactory(any(String.class))).thenReturn(bindingFactory);
        when(
            bindingFactory.createBindingInfo(any(org.apache.cxf.service.Service.class), any(String.class),
                any(Object.class))).thenReturn(bindingInfo);
        when(bindingFactory.createBinding(bindingInfo)).thenReturn(binding);
        when(conduitInitiatorManager.getConduitInitiator("http://schemas.xmlsoap.org/wsdl/http/")).thenReturn(
            conduitInitiator);
        when(conduitInitiator.getConduit(any(EndpointInfo.class))).thenReturn(conduit);

        clientWrapper = new ClientWrapper(bus, immutableEndpoint);

        assertNotNull(clientWrapper.getClient());
        assertNotNull(clientWrapper.getClient().getInInterceptors());
        assertEquals(3, clientWrapper.getClient().getInInterceptors().size());
        assertEquals(4, clientWrapper.getClient().getOutInterceptors().size());
        verify(conduit).setMessageObserver(any(MessageObserver.class));
        verify(conduit).setCloseInput(false);
        commonPostExecutionVerifications();
    }

    public void testGetOperation_correctOperation() throws Exception
    {
        String validOperationName = "someOperation";
        String actualOperationName = "someOperation";

        assertResults(validOperationName, actualOperationName);
    }

    public void testGetOperation_correctOperationWithCapitalFistLetter() throws Exception
    {
        String validOperationName = "SomeOperation";
        String actualOperationName = "someOperation";

        assertResults(validOperationName, actualOperationName);
    }

    private void assertResults(String validOperationName, String actualOperationName) throws Exception
    {
        ClientWrapper clientWrapper = new ClientWrapper(this.immutableEndpoint);
        BindingInfo bindingInfo = configureExpectationsForBindingInfo(clientWrapper);

        BindingOperationInfo expectedOperation = mock(BindingOperationInfo.class);
        when(bindingInfo.getOperation(new QName(validOperationName))).thenReturn(expectedOperation);

        BindingOperationInfo operation = clientWrapper.getOperation(actualOperationName);
        assertSame(expectedOperation, operation);
    }

    public void testGetOperation_incorrectOperation() throws Exception
    {
        String validOperationName = "someOperation";
        String actualOperationName = "someOtherOperation";

        ClientWrapper clientWrapper = new ClientWrapper(this.immutableEndpoint);
        BindingInfo bindingInfo = configureExpectationsForBindingInfo(clientWrapper);

        BindingOperationInfo expectedOperation = mock(BindingOperationInfo.class);
        when(bindingInfo.getOperation(new QName(validOperationName))).thenReturn(expectedOperation);

        try
        {
            clientWrapper.getOperation(actualOperationName);
            fail("It should have thrown exception");
        }
        catch (Exception e)
        {
            assertNotNull(e.getMessage());
            assertTrue(e.getMessage().contains(actualOperationName));
        }
    }

    private BindingInfo configureExpectationsForBindingInfo(ClientWrapper clientWrapper)
    {
        clientWrapper.client = mock(Client.class);
        org.apache.cxf.service.Service service = mock(org.apache.cxf.service.Service.class);
        Binding binding = mock(Binding.class);
        BindingInfo bindingInfo = mock(BindingInfo.class);

        when(clientWrapper.client.getEndpoint()).thenReturn(this.endpoint);
        when(this.endpoint.getService()).thenReturn(service);
        when(service.getName()).thenReturn(new QName("someLocalPart"));
        when(this.endpoint.getBinding()).thenReturn(binding);
        when(binding.getBindingInfo()).thenReturn(bindingInfo);
        return bindingInfo;
    }
}

class DummyService extends Service
{
    static Client client = mock(Client.class);

    private InvocationHandler invocationHandler;

    public DummyService()
    {
        super(null, null);

        invocationHandler = new ClientProxy(client)
        {
            Map<String, String> requestContext = new HashMap<String, String>();

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
            {
                if ("getRequestContext".equals(method.getName()))
                {
                    return requestContext;
                }
                return null;
            }
        };
    }

    protected DummyService(URL wsdlDocumentLocation, QName serviceName)
    {
        super(wsdlDocumentLocation, serviceName);
    }

    @WebEndpoint(name = "Port")
    public BindingProvider getBindingProvider()
    {
        return (BindingProvider) Proxy.newProxyInstance(this.getClass().getClassLoader(),
            new Class[]{BindingProvider.class}, invocationHandler);
    }

}
