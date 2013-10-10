/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http.construct.builder;

import org.mule.api.MuleException;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transport.http.construct.HttpProxy;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

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
