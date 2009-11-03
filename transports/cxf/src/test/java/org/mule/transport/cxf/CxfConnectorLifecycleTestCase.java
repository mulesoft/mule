/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf;

import org.mule.api.MuleException;
import org.mule.api.service.Service;
import org.mule.tck.FunctionalTestCase;

public class CxfConnectorLifecycleTestCase extends FunctionalTestCase
{

    private static String SERVICE_NAME = "echoService";
    private static String PROTOCOL_SERVICE_NAME = "_cxfServiceComponent{http|//localhost|63081/services/Echo}EchoComponent";

    @Override
    protected String getConfigResources()
    {
        return "basic-conf.xml";
    }

    /**
     * MULE-4570
     * 
     * @throws MuleException
     */
    public void testStopService() throws MuleException
    {
        Service service = muleContext.getRegistry().lookupService(SERVICE_NAME);
        Service protocolService = muleContext.getRegistry().lookupService(PROTOCOL_SERVICE_NAME);
        assertNotNull(service);
        assertTrue(service.isStarted());
        assertNotNull(protocolService);
        assertTrue(protocolService.isStarted());

        service.stop();

        assertFalse(service.isStarted());
        assertFalse(protocolService.isStarted());
        assertNull(muleContext.getRegistry().lookupService(PROTOCOL_SERVICE_NAME));
    }

    /**
     * MULE-4569
     * 
     * @throws MuleException
     */
    public void testRestartService() throws MuleException
    {
        Service service = muleContext.getRegistry().lookupService(SERVICE_NAME);
        Service protocolService = muleContext.getRegistry().lookupService(PROTOCOL_SERVICE_NAME);

        service.stop();
        service.start();

        // protocolService is recreated when service is restarted
        protocolService = muleContext.getRegistry().lookupService(PROTOCOL_SERVICE_NAME);

        assertTrue(service.isStarted());
        assertTrue(protocolService.isStarted());
        assertNotNull(muleContext.getRegistry().lookupService(PROTOCOL_SERVICE_NAME));

    }

}
