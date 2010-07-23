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
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.context.WorkManager;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.retry.RetryCallback;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.api.transformer.TransformerException;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.transport.NullPayload;

public class CxfMessageDispatcherTestCase extends AbstractMuleTestCase
{
    private CxfMessageDispatcher cxfMessageDispatcher;
    private RetryPolicyTemplate retryPolicyTemplate;
    private CxfConnector connector;
    private OutboundEndpoint endpoint;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        endpoint = mock(OutboundEndpoint.class);
        connector = new CxfConnector(muleContext);
        retryPolicyTemplate = mock(RetryPolicyTemplate.class);

        when(endpoint.getConnector()).thenReturn(connector);
        when(endpoint.getRetryPolicyTemplate()).thenReturn(retryPolicyTemplate);

        connector.initialise();
        cxfMessageDispatcher = new CxfMessageDispatcher(endpoint);
    }

    public void testCanRunAFullLifeCycle() throws Exception
    {
        cxfMessageDispatcher.initialise();
        cxfMessageDispatcher.connect();
        cxfMessageDispatcher.start();
        cxfMessageDispatcher.stop();
        cxfMessageDispatcher.disconnect();
        cxfMessageDispatcher.dispose();

        verify(retryPolicyTemplate, atLeastOnce()).execute(any(RetryCallback.class), any(WorkManager.class));
    }

    public void testGetArgs_withObjectAsPayload() throws Exception
    {
        Object payload = new Object();

        Object[] args = callGetArgsWithPayload(payload);

        assertNotNull(args);
        assertEquals(1, args.length);
        assertSame(payload, args[0]);
    }

    public void testGetArgs_withArrayAsPayload() throws Exception
    {
        Object[] payload = new Object[4];

        Object[] args = callGetArgsWithPayload(payload);

        assertSame(payload, args);
    }

    public void testGetArgs_withNullPayloadAsPayload() throws Exception
    {
        Object payload = NullPayload.getInstance();

        Object[] args = callGetArgsWithPayload(payload);

        assertNotNull(args);
        assertEquals(1, args.length);
        assertSame(payload, args[0]);
    }

    private Object[] callGetArgsWithPayload(Object payload) throws TransformerException
    {
        MuleEvent muleEvent = mock(MuleEvent.class);
        ClientWrapper clientWrapper = mock(ClientWrapper.class);
        MuleMessage muleMessage = mock(MuleMessage.class);

        when(muleEvent.getMessage().getPayload()).thenReturn(payload);
        when(muleEvent.getMessage()).thenReturn(muleMessage);

        cxfMessageDispatcher.wrapper = clientWrapper;
        Object[] args = cxfMessageDispatcher.getArgs(muleEvent);
        return args;
    }

}

