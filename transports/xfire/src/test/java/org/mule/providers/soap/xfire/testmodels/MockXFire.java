/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.xfire.testmodels;

import java.io.OutputStream;
import java.util.List;

import org.codehaus.xfire.XFire;
import org.codehaus.xfire.service.DefaultServiceRegistry;
import org.codehaus.xfire.service.ServiceRegistry;
import org.codehaus.xfire.transport.DefaultTransportManager;
import org.codehaus.xfire.transport.TransportManager;

/**
 * This class is only used in the namespace handler test that needs a valid XFire 
 * implementation class to instantiate.
 */
public class MockXFire implements XFire
{
    public void generateWSDL(String service, OutputStream out)
    {
        // does nothing
    }

    public List getInPhases()
    {
        return null;
    }

    public List getOutPhases()
    {
        return null;
    }

    public Object getProperty(String key)
    {
        return null;
    }

    public ServiceRegistry getServiceRegistry()
    {
        return new DefaultServiceRegistry();
    }

    public TransportManager getTransportManager()
    {
        return new DefaultTransportManager();
    }

    public void setProperty(String key, Object value)
    {
        // ignore
    }

    public List getFaultHandlers()
    {
        return null;
    }

    public List getInHandlers()
    {
        return null;
    }

    public List getOutHandlers()
    {
        return null;
    }
}
