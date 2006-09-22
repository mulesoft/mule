/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.ejb;

import org.mule.providers.rmi.RmiConnector;
import org.mule.umo.UMOComponent;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.provider.UMOMessageReceiver;

/**
 * Code by (c) 2005 P.Oikari.
 *
 * @author <a href="mailto:tsuppari@yahoo.co.uk">P.Oikari</a>
 * @version $Revision$
 */

public class EjbConnector extends RmiConnector
{
    ////////////////////////////////////////////
    // Errorcodes
    ///////////////////////////////////////////
    public static final int EJB_SERVICECLASS_INVOCATION_FAILED = 2;

    private long pollingFrequency = 1000;

    private String receiverArgumentClass = null;

    private EjbAble ejbAble = null;

    public String getProtocol()
    {
        return "ejb";
    }

    public UMOMessageReceiver createReceiver(UMOComponent component, UMOEndpoint endpoint) throws Exception
    {
        final Object[] args = new Object[]{new Long(pollingFrequency)};
        return getServiceDescriptor().createMessageReceiver(this, component, endpoint, args);
    }

    ////////////////////////////////////////////////////
    //  Receiver method + args
    ///////////////////////////////////////////////////
    public long getPollingFrequency()
    {
        return pollingFrequency;
    }

    public void setPollingFrequency(long pollingFrequency)
    {
        this.pollingFrequency = pollingFrequency;
    }

    public String getReceiverArgumentClass()
    {
        return receiverArgumentClass;
    }

    public void setReceiverArgumentClass(String className) throws Exception
    {
        this.receiverArgumentClass = className;
    }

    public EjbAble getEjbAble()
    {
        return ejbAble;
    }

    public void setEjbAble(EjbAble ejbAble)
    {
        this.ejbAble = ejbAble;
    }
}
