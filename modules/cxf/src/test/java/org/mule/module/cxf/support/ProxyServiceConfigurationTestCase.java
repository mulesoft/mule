/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf.support;

import org.mule.MuleServer;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.util.ExceptionUtils;

import java.net.URL;

import org.apache.cxf.service.factory.ServiceConstructionException;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ProxyServiceConfigurationTestCase extends AbstractMuleTestCase
{

    @Test
    public void testGetEndpointName_CorrectNameSpace()
    {
        String configFilePath = "/org/mule/module/cxf/support/test-proxy-mule-config-correct-namespace.xml";
        startServer(configFilePath);
    }

    @Test
    public void testGetEndpointName_NoNameSpace()
    {
        String configFilePath = "/org/mule/module/cxf/support/test-proxy-mule-config-no-namespace.xml";
        try
        {
            startServer(configFilePath);
            fail("It should have failed because no namespace was specified");
        }
        catch (RuntimeException e)
        {
            Throwable rootCause = ExceptionUtils.getRootCause(e);
            assertTrue("Exception must be of type " + ServiceConstructionException.class + ", instead of "
                       + rootCause, rootCause instanceof ServiceConstructionException);
        }
    }

    private void startServer(String configFilePath)
    {
        URL configURL = this.getClass().getResource(configFilePath);
        MuleServer muleServer = new MuleServer(configURL.toString())
        {
            @Override
            public void shutdown(Throwable e)
            {
                throw new RuntimeException(e);
            }
        };
        muleServer.start(false, false);
    }

}
