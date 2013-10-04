/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.tcp.issues;

import org.mule.transport.tcp.TcpConnector;
import org.mule.transport.tcp.TcpFunctionalTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertTrue;

/**
 * This is just to check that the Boolean (rather than boolean) doesn't cause any problems
 */
public class ReuseMule2069TestCase extends TcpFunctionalTestCase
{
    public ReuseMule2069TestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }
    
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "reuse-mule-2069-service.xml"},
            {ConfigVariant.FLOW, "reuse-mule-2069-flow.xml"}
        });
    }

    @Test
    public void testReuseSetOnConnector()
    {
        assertTrue(((TcpConnector) muleContext.getRegistry().lookupConnector(TcpConnector.TCP)).isReuseAddress().booleanValue());
    }

}
