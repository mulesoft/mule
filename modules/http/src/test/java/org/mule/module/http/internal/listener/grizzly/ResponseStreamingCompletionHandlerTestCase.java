/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener.grizzly;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.module.http.internal.listener.grizzly.ResponseStreamingCompletionHandler.MULE_CLASSLOADER;

import org.glassfish.grizzly.attributes.AttributeHolder;
import org.junit.Test;
import org.mule.module.http.internal.domain.InputStreamHttpEntity;
import org.mule.module.http.internal.domain.response.DefaultHttpResponse;
import org.mule.module.http.internal.domain.response.ResponseStatus;

import java.io.InputStream;

import org.apache.commons.collections.MultiMap;
import org.glassfish.grizzly.Transport;

public class ResponseStreamingCompletionHandlerTestCase extends BaseResponseCompletionHandlerTestCase
{

    private ResponseStreamingCompletionHandler handler;
    private AttributeHolder attributeHolder = spy(AttributeHolder.class);

    @Override
    public void setUp()
    {
        super.setUp();
        when(connection.getTransport()).thenReturn(mock(Transport.class, RETURNS_DEEP_STUBS));
        InputStream mockStream = mock(InputStream.class);
        handler = new ResponseStreamingCompletionHandler(ctx,
                                                         request,
                                                         new DefaultHttpResponse(mock(ResponseStatus.class),
                                                                                mock(MultiMap.class),
                                                                                new InputStreamHttpEntity(mockStream)),
                                                         callback);
        when(connection.getAttributes()).thenReturn(attributeHolder);

    }

    @Override
    protected BaseResponseCompletionHandler getHandler()
    {
        return handler;
    }

    @Test
    public void failedTaskAvoidsResponse()
    {
        super.failedTaskAvoidsResponse();
        verify(attributeHolder, times(1)).removeAttribute(MULE_CLASSLOADER);
    }

    @Test
    public void cancelledTaskResponse()
    {
       super.cancelledTaskResponse();
       verify(attributeHolder, times(1)).removeAttribute(MULE_CLASSLOADER);

    }
}
