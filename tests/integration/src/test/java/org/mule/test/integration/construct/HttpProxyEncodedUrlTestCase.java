/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.construct;

import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.integration.endpoints.AbstractEndpointEncodedUrlTestCase;

import org.junit.Rule;

public class HttpProxyEncodedUrlTestCase extends AbstractEndpointEncodedUrlTestCase
{

    @Rule
    public DynamicPort port1 = new DynamicPort("port1");

    @Rule
    public DynamicPort port2 = new DynamicPort("port2");

    @Rule
    public DynamicPort port3 = new DynamicPort("port3");

    @Rule
    public DynamicPort port4 = new DynamicPort("port4");

    @Override
    protected String getEncodedUrlConfigFile()
    {
        return "org/mule/test/integration/construct/http-proxy-encoded-url-config.xml";
    }

    @Override
    protected String getDynamicUrl()
    {
        return createInputUrl(port1.getNumber());
    }

    @Override
    protected String getAssembledDynamicUrl()
    {
        return createInputUrl(port2.getNumber());
    }

    @Override
    protected String getStaticUrl()
    {
        return createInputUrl(port3.getNumber());
    }

    @Override
    protected String getAssembledStaticUrl()
    {
        return createInputUrl(port4.getNumber());
    }

    private String createInputUrl(int port)
    {
        return "http://localhost:" + port;
    }
}
