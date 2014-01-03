/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.ws.security;

import org.mule.module.cxf.builder.ProxyClientMessageProcessorBuilder;

import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.ws.security.handler.WSHandlerConstants;

public class WssUsernameTokenSecurityStrategy implements SecurityStrategy
{

    protected String username;
    protected String password;
    protected String passwordType;

    @Override
    public void apply(ProxyClientMessageProcessorBuilder builder)
    {
        Map<String, Object> configProperties = new HashMap<String, Object>();
        configProperties.put(WSHandlerConstants.ACTION, WSHandlerConstants.USERNAME_TOKEN);
        configProperties.put(WSHandlerConstants.ACTION, WSHandlerConstants.USERNAME_TOKEN);
        builder.getClient().getOutInterceptors().add(new WSS4JOutInterceptor(configProperties));

    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getUsername()
    {
        return username;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public String getPasswordType()
    {
        return passwordType;
    }

    public void setPasswordType(String passwordType)
    {
        this.passwordType = passwordType;
    }

}
