/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.api.oauth.callback;

import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transport.Connector;
import org.mule.util.NumberUtils;

import org.apache.log4j.Logger;

public class DefaultHttpCallbackAdapter implements Initialisable, HttpCallbackAdapter
{

    private Integer localPort;
    private Integer remotePort;
    private String domain;
    private String path;
    private Connector connector;
    private final static Logger LOGGER = Logger.getLogger(DefaultHttpCallbackAdapter.class);
    private Boolean async = false;

    /**
     * Retrieves localPort
     */
    public Integer getLocalPort()
    {
        return this.localPort;
    }

    /**
     * Sets localPort
     * 
     * @param value Value to set
     */
    public void setLocalPort(Integer value)
    {
        this.localPort = value;
    }

    /**
     * Retrieves remotePort
     */
    public Integer getRemotePort()
    {
        return this.remotePort;
    }

    /**
     * Sets remotePort
     * 
     * @param value Value to set
     */
    public void setRemotePort(Integer value)
    {
        this.remotePort = value;
    }

    /**
     * Retrieves domain
     */
    public String getDomain()
    {
        return this.domain;
    }

    /**
     * Sets domain
     * 
     * @param value Value to set
     */
    public void setDomain(String value)
    {
        this.domain = value;
    }

    /**
     * Retrieves path
     */
    public String getPath()
    {
        return this.path;
    }

    /**
     * Sets path
     * 
     * @param value Value to set
     */
    public void setPath(String value)
    {
        this.path = value;
    }

    /**
     * Retrieves connector
     */
    public Connector getConnector()
    {
        return this.connector;
    }

    /**
     * Sets connector
     * 
     * @param value Value to set
     */
    public void setConnector(Connector value)
    {
        this.connector = value;
    }

    /**
     * Retrieves async
     */
    public Boolean getAsync()
    {
        return this.async;
    }

    /**
     * Sets async
     * 
     * @param value Value to set
     */
    public void setAsync(Boolean value)
    {
        this.async = value;
    }

    public void initialise() throws InitialisationException
    {
        if (localPort == null)
        {
            String portSystemVar = System.getProperty("http.port");
            if (NumberUtils.isDigits(portSystemVar))
            {
                localPort = Integer.parseInt(portSystemVar);
            }
            else
            {
                LOGGER.warn("Environment variable 'http.port' not found, using default localPort: 8080");
                localPort = 8080;
            }
        }
        if (remotePort == null)
        {
            LOGGER.info("Using default remotePort: 80");
            remotePort = 80;
        }
        if (domain == null)
        {
            String domainSystemVar = System.getProperty("fullDomain");
            if (domainSystemVar != null)
            {
                domain = domainSystemVar;
            }
            else
            {
                LOGGER.warn("Environment variable 'fullDomain' not found, using default: localhost");
                domain = "localhost";
            }
        }
    }

}
