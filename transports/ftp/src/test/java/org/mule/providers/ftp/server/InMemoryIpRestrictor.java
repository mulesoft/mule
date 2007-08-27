/*
 * $Id:InMemoryIpRestrictor.java 7261 2007-06-27 02:23:03Z aperepel $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.ftp.server;

import java.net.InetAddress;

import org.apache.ftpserver.ftplet.Component;
import org.apache.ftpserver.ftplet.Configuration;
import org.apache.ftpserver.interfaces.IpRestrictor;


/**
 * This class is needed to avoid creating unnesessary configuration files while running ftp transport tests.
 */

public class InMemoryIpRestrictor implements IpRestrictor, Component
{

    /**
     * Configure the IP restrictor.
     *
     * @param config ignored
     */
    public void configure(Configuration config)
    {
    }

    /**
     * Has the permission?
     *
     * @param address ignored
     * @return true
     */
    public boolean hasPermission(InetAddress address)
    {
        return true;
    }

    /**
     * Get permission array.
     *
     * @return empty array
     */
    public Object[][] getPermissions()
    {
        return new Object[0][2];
    }

    /**
     * Set permission array.
     *
     * @param permissions ignored
     */
    public void setPermissions(Object[][] permissions)
    {
    }

    /**
     * Release all the resources
     */
    public void dispose()
    {
    }
}
