/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.soap.axis;

import org.mule.api.MuleException;
import org.mule.tck.FunctionalTestCase;

public class AxisConnectorLifecycleTestCase extends FunctionalTestCase
{

    private static String SERVICE_NAME = "mycomponent";
    private static String PROTOCOL_SERVICE_NAME = AxisConnector.AXIS_SERVICE_PROPERTY + "connector.axis.0";

    @Override
    protected String getConfigResources()
    {
        return "axis-http-mule-config.xml";
    }

    /**
     * MULE-4570, MULE-4573
     * 
     * @throws MuleException
     */
    public void testStopService() throws MuleException
    {
        muleContext.getRegistry().lookupService(SERVICE_NAME).stop();
        assertFalse(muleContext.getRegistry().lookupService(SERVICE_NAME).isStarted());
        assertFalse(muleContext.getRegistry().lookupService(PROTOCOL_SERVICE_NAME).isStarted());

    }

    /**
     * MULE-4570, MULE-4573
     * 
     * @throws MuleException
     */
    public void testDisposeService() throws MuleException
    {
        muleContext.getRegistry().lookupService(SERVICE_NAME).dispose();
        assertFalse(muleContext.getRegistry().lookupService(SERVICE_NAME).isStarted());
        assertNull(muleContext.getRegistry().lookupService(PROTOCOL_SERVICE_NAME));

    }

    /**
     * MULE-4569, MULE-4573
     * 
     * @throws MuleException
     */
    public void testRestartService() throws MuleException
    {
        muleContext.getRegistry().lookupService(SERVICE_NAME).stop();
        assertFalse(muleContext.getRegistry().lookupService(SERVICE_NAME).isStarted());
        assertFalse(muleContext.getRegistry().lookupService(PROTOCOL_SERVICE_NAME).isStarted());
        muleContext.getRegistry().lookupService(SERVICE_NAME).start();
        assertTrue(muleContext.getRegistry().lookupService(SERVICE_NAME).isStarted());
        assertTrue(muleContext.getRegistry().lookupService(PROTOCOL_SERVICE_NAME).isStarted());

    }

}
