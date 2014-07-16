/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.construct.builder;

import static org.junit.Assert.assertEquals;
import org.mule.api.MuleException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transport.http.construct.HttpProxy;

import org.junit.Test;

public class HttpProxyBuilderTestCase extends AbstractMuleContextTestCase
{
    @Test
    public void testConfigurationNoCache() throws MuleException
    {
        final HttpProxy httpProxy = new HttpProxyBuilder().name("test-http-proxy-no-cache")
            .inboundAddress("test://foo")
            .outboundAddress("test://bar")
            .build(muleContext);

        assertEquals("test-http-proxy-no-cache", httpProxy.getName());
    }

    // TODO test configuration with cache
}
