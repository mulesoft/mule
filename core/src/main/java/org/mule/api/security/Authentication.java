/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.security;

import org.mule.api.MuleEvent;

import java.io.Serializable;
import java.util.Map;

/**
 * <code>Authentication</code> represents an authentication request and contains
 * authentication information if the request was successful
 */
public interface Authentication extends Serializable
{
    void setAuthenticated(boolean b);

    boolean isAuthenticated();

    Object getCredentials();

    Object getPrincipal();

    Map<String, Object> getProperties();

    void setProperties(Map<String, Object> properties);

    MuleEvent getEvent();

    void setEvent(MuleEvent event);
}
