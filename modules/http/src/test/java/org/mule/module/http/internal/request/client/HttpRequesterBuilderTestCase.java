/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.http.internal.request.client;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mule.module.http.internal.request.client.HttpRequesterBuilder.DEFAULT_HTTP_REQUEST_CONFIG_NAME;
import org.mule.module.http.internal.request.DefaultHttpRequesterConfig;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Before;
import org.junit.Test;

public class HttpRequesterBuilderTestCase extends AbstractMuleContextTestCase
{

    private final DefaultHttpRequesterConfig requesterConfig = mock(DefaultHttpRequesterConfig.class);
    private HttpRequesterBuilder builder;

    @Before
    public void setUp() throws Exception
    {
         builder = new HttpRequesterBuilder(muleContext);
         builder.setUrl("http://someport:1234");
    }

    @Test
    public void requestConfigIsStartedAfterBeenTakenFromRegistry() throws Exception
    {
        muleContext.getRegistry().registerObject(DEFAULT_HTTP_REQUEST_CONFIG_NAME, requesterConfig);
        builder.build();
        verify(requesterConfig).start();
    }
}