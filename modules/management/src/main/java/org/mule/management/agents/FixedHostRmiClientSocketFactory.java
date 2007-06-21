/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.management.agents;

import org.mule.util.StringUtils;

import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.rmi.server.RMIClientSocketFactory;

/**
 * This implementation will enforce specific overrideHost/ip for RMI calls on multi-NIC servers.
 * TODO MULE-1440 this should probably be moved into the RMI transport.
 */
public class FixedHostRmiClientSocketFactory implements RMIClientSocketFactory, Serializable
{
    /**
     * Host to use instead of the default one.
     */
    private String overrideHost;

    /**
     * Default constructor.
     */
    public FixedHostRmiClientSocketFactory ()
    {
    }

    /**
     * Create a new instance.
     * @param overrideHost host/ip to enforce
     */
    public FixedHostRmiClientSocketFactory (final String overrideHost)
    {
        this.overrideHost = overrideHost;
    }

    /**
     * Create a client socket connected to the specified overrideHost and port.
     *
     * @param host the host name IGNORED if an override configured
     * @param port the port number
     * @return a socket connected to the specified overrideHost and port.
     * @throws java.io.IOException if an I/O error occurs during socket creation
     */
    public Socket createSocket (String host, int port) throws IOException
    {
        final String hostToUse = StringUtils.defaultString(overrideHost, host);
        return new Socket(hostToUse, port);
    }

    /**
     * Getter for property 'overrideHost'.
     *
     * @return Value for property 'overrideHost'.
     */
    public String getOverrideHost ()
    {
        return overrideHost;
    }

    /**
     * Setter for property 'overrideHost'.
     *
     * @param overrideHost Value to set for property 'overrideHost'.
     */
    public void setOverrideHost (final String overrideHost)
    {
        this.overrideHost = overrideHost;
    }
}
