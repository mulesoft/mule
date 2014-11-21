/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.security.oauth.callback;

/**
 * Adapter interface for Http callbacks
 */
public interface HttpCallbackAdapter
{

    /**
     * Retrieves localPort
     */
    public Integer getLocalPort();

    /**
     * Retrieves remotePort
     */
    public Integer getRemotePort();

    /**
     * Retrieves domain
     */
    public String getDomain();

    /**
     * Retrieves connector
     */
    public Object getConnector();

    /**
     * Retrieves async
     */
    public Boolean getAsync();

    /**
     * Retrieves path
     */
    public String getPath();

    /**
     * Sets localPort
     * 
     * @param value Value to set
     */
    public void setLocalPort(Integer value);

    /**
     * Sets remotePort
     * 
     * @param value Value to set
     */
    public void setRemotePort(Integer value);

    /**
     * Sets domain
     * 
     * @param value Value to set
     */
    public void setDomain(String value);

    /**
     * Sets path
     * 
     * @param value Value to set
     */
    public void setPath(String value);

    /**
     * Sets connector
     * 
     * @param value Value to set
     */
    public void setConnector(Object value);

    /**
     * Sets async
     * 
     * @param value Value to set
     */
    public void setAsync(Boolean value);

}
