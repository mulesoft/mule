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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.DefaultMuleMessage;
import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.transport.OutputHandler;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.transport.cxf.support.DelegatingOutputStream;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.soap.SoapConstants;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.interceptor.InterceptorChain;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.transport.local.LocalConduit;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class CxfServiceComponentTestCase extends AbstractMuleTestCase
{
    private CxfServiceComponent cxfServiceComponent;
    private CxfMessageReceiver receiver;
    private Message outMessage;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        outMessage = new MessageImpl();

        receiver = mock(CxfMessageReceiver.class);
        final Bus cxfBus = mock(Bus.class);
        receiver.connector = new CxfConnector(muleContext);
        receiver.connector.setCxfBus(cxfBus);
        final CxfConnector connector = new CxfConnector(muleContext);

        cxfServiceComponent = new CxfServiceComponent(connector, receiver);
    }

    /**
     * This test tests some parts of the method
     * {@link CxfServiceComponent#sendToDestination(MuleEventContext)}. Its final
     * objective is to test the code inside the inner {@link OutputHandler}
     * implementation.
     * 
     * @throws Exception
     */
    public void testSendToDestination() throws Exception
    {
        OutputHandler outputHandlerGottenSendToDestination = executeAndValidateCallToSentToDestination();

        executeAndValidateCallToWriteOn(outputHandlerGottenSendToDestination);
    }

    /**
     * This makes a call to
     * {@link CxfServiceComponent#sendToDestination(MuleEventContext)} and returns
     * the inner {@link OutputHandler} that is instantiated inside that method.
     * 
     * @return the inner {@link OutputHandler} that is instantiated inside
     *         {@link CxfServiceComponent#sendToDestination(MuleEventContext)}.
     * @throws MuleException
     * @throws IOException
     */
    private OutputHandler executeAndValidateCallToSentToDestination() throws MuleException, IOException
    {
        // create mocks
        final MuleEventContext ctx = mock(MuleEventContext.class);
        final String method = "GET";
        final Server server = mock(Server.class);
        final Destination destination = mock(Destination.class);
        final InboundEndpoint endpoint = mock(InboundEndpoint.class);
        final MessageObserver messageObserver = mock(MessageObserver.class);
        final String someActionProperty = "someActionProperty";
        final String path = "somePath";
        final String basePath = "someBasePath";
        
        final MuleMessage muleReqMsg = new DefaultMuleMessage("some object", muleContext);
        muleReqMsg.setProperty(HttpConnector.HTTP_METHOD_PROPERTY, method);
        muleReqMsg.setProperty(HttpConnector.HTTP_REQUEST_PATH_PROPERTY, path);
        muleReqMsg.setProperty(HttpConnector.HTTP_CONTEXT_PATH_PROPERTY, basePath);
        muleReqMsg.setProperty(SoapConstants.SOAP_ACTION_PROPERTY, "\"" + someActionProperty + "\"");

        // configure expectations.
        when(ctx.getMessage()).thenReturn(muleReqMsg);
        when(ctx.getMuleContext()).thenReturn(muleContext);
        when(receiver.getServer()).thenReturn(server);
        when(server.getDestination()).thenReturn(destination);
        when(receiver.getEndpoint()).thenReturn(endpoint);
        when(destination.getMessageObserver()).thenReturn(messageObserver);
        doAnswer(new Answer<Object>()
        {
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                MessageImpl messageImpl = (MessageImpl) invocation.getArguments()[0];
                assertNotNull(messageImpl);
                assertSame(muleReqMsg, messageImpl.get(CxfConstants.MULE_MESSAGE));
                assertEquals(someActionProperty,
                    messageImpl.get(org.mule.transport.soap.SoapConstants.SOAP_ACTION_PROPERTY_CAPS));
                assertEquals(Boolean.TRUE, messageImpl.get(LocalConduit.DIRECT_DISPATCH));
                assertSame(RequestContext.getEvent(), messageImpl.get(MuleProperties.MULE_EVENT_PROPERTY));
                assertSame(destination, messageImpl.getDestination());
                assertSame(method, messageImpl.get(Message.HTTP_REQUEST_METHOD));
                assertSame(path, messageImpl.get(Message.PATH_INFO));
                assertSame(basePath, messageImpl.get(Message.BASE_PATH));
                
                messageImpl.getExchange().setOutMessage(outMessage);
                return null;
            }
        }).when(messageObserver).onMessage(any(MessageImpl.class));

        // do method call
        Object result = cxfServiceComponent.sendToDestination(ctx);

        // check post conditions
        verify(messageObserver).onMessage(any(MessageImpl.class));
        assertNotNull(result);
        assertTrue(result instanceof DefaultMuleMessage);
        DefaultMuleMessage muleResMsg = (DefaultMuleMessage) result;
        assertTrue(muleResMsg.getPayload() instanceof OutputHandler);
        return (OutputHandler) muleResMsg.getPayload();
    }

    /**
     * This method calls and validates the
     * {@link OutputHandler#write(MuleEvent, OutputStream)} method on the instance
     * returned by {@link #executeAndValidateCallToSentToDestination()}.
     * 
     * @param outputHandlerGottenSendToDestination
     * @throws IOException
     */
    private void executeAndValidateCallToWriteOn(OutputHandler outputHandlerGottenSendToDestination)
        throws IOException
    {
        // create mocks for validations on returned object
        final MuleEvent event = mock(MuleEvent.class);
        final OutputStream out = mock(OutputStream.class);
        final DelegatingOutputStream delegate = mock(DelegatingOutputStream.class);
        final InterceptorChain interceptorChain = mock(InterceptorChain.class);
        final InOrder inOrderChecker = inOrder(delegate, out, interceptorChain);

        // configure expectations for executing method on returned object.
        outMessage.setContent(DelegatingOutputStream.class, delegate);
        outMessage.setInterceptorChain(interceptorChain);
        when(delegate.getOutputStream()).thenReturn(new ByteArrayOutputStream());

        // execute method on returned object
        outputHandlerGottenSendToDestination.write(event, out);

        // check post conditions on method for returned objects
        inOrderChecker.verify(delegate).getOutputStream();
        inOrderChecker.verify(out).write(any(byte[].class));
        inOrderChecker.verify(delegate).setOutputStream(out);
        inOrderChecker.verify(out).flush();
    }
}


