/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
