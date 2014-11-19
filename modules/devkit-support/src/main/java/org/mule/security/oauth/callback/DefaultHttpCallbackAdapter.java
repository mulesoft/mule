/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.security.oauth.callback;

import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.util.NumberUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DefaultHttpCallbackAdapter implements Initialisable, HttpCallbackAdapter
{

    private final static Logger LOGGER = LoggerFactory.getLogger(DefaultHttpCallbackAdapter.class);
    private static final int DEFAULT_LOCAL_PORT = 8080;
    private static final int DEFAULT_REMOTE_PORT = 80;

    private Integer localPort;
    private Integer remotePort;
    private String domain;
    private String path;
    private Object connector;
    private Boolean async = false;

    /**
     * {@inheritDoc}
     */
    public Integer getLocalPort()
    {
        return this.localPort;
    }

    /**
     * {@inheritDoc}
     */
    public void setLocalPort(Integer value)
    {
        this.localPort = value;
    }

    /**
     * {@inheritDoc}
     */
    public Integer getRemotePort()
    {
        return this.remotePort;
    }

    /**
     * {@inheritDoc}
     */
    public void setRemotePort(Integer value)
    {
        this.remotePort = value;
    }

    /**
     * {@inheritDoc}
     */
    public String getDomain()
    {
        return this.domain;
    }

    /**
     * {@inheritDoc}
     */
    public void setDomain(String value)
    {
        this.domain = value;
    }

    /**
     * {@inheritDoc}
     */
    public String getPath()
    {
        return this.path;
    }

    /**
     * {@inheritDoc}
     */
    public void setPath(String value)
    {
        this.path = value;
    }

    /**
     * {@inheritDoc}
     */
    public Object getConnector()
    {
        return this.connector;
    }

    /**
     * {@inheritDoc}
     */
    public void setConnector(Object value)
    {
        this.connector = value;
    }

    /**
     * {@inheritDoc}
     */
    public Boolean getAsync()
    {
        return this.async;
    }

    /**
     * {@inheritDoc}
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
                localPort = DEFAULT_LOCAL_PORT;
            }
        }
        if (remotePort == null)
        {
            LOGGER.info("Using default remotePort: 80");
            remotePort = DEFAULT_REMOTE_PORT;
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
