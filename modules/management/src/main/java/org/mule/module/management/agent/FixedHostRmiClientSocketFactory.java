/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.management.agent;

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
        /* NOTE this is StringUtils.defaultIfEmpty(overrideHost, host)
           This socket factory is required on the client, minimize the dependency graph
        */
        final String hostToUse = (overrideHost == null || overrideHost.trim().length() == 0) ? host : overrideHost;

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
