/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
