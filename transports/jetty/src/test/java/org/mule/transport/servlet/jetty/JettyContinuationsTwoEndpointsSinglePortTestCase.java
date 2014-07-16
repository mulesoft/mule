/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet.jetty;

public class JettyContinuationsTwoEndpointsSinglePortTestCase extends JettyTwoEndpointsSinglePortTestCase
{
    public JettyContinuationsTwoEndpointsSinglePortTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Override
    protected String getConfigFile()
    {
        return "jetty-continuations-two-endpoints-single-port.xml";
    }

}
