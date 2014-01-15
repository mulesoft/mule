/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.ws.security;

import org.mule.module.cxf.builder.ProxyClientMessageProcessorBuilder;
import org.mule.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSPasswordCallback;
import org.apache.ws.security.handler.WSHandlerConstants;

public class WssUsernameTokenSecurityStrategy implements SecurityStrategy
{

    private String username;
    private String password;
    private PasswordType passwordType;
    private boolean addNonce;
    private boolean addCreated;

    @Override
    public void apply(ProxyClientMessageProcessorBuilder builder)
    {
        Map<String, Object> configProperties = new HashMap<String, Object>();
        configProperties.put(WSHandlerConstants.ACTION, WSHandlerConstants.USERNAME_TOKEN);
        configProperties.put(WSHandlerConstants.USER, username);

        if (passwordType == PasswordType.TEXT)
        {
            configProperties.put(WSHandlerConstants.PASSWORD_TYPE, "PasswordText");
        }
        else if (passwordType == PasswordType.DIGEST)
        {
            configProperties.put(WSHandlerConstants.PASSWORD_TYPE, "PasswordDigest");
        }

        List<String> additionalElements = new ArrayList<String>(2);
        if (addNonce)
        {
            additionalElements.add(WSConstants.NONCE_LN);
        }
        if (addCreated)
        {
            additionalElements.add(WSConstants.CREATED_LN);
        }
        if (!additionalElements.isEmpty())
        {
            configProperties.put(WSHandlerConstants.ADD_UT_ELEMENTS, StringUtils.join(additionalElements, " "));
        }

        configProperties.put(WSHandlerConstants.PW_CALLBACK_REF, new CallbackHandler()
        {
            @Override
            public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException
            {
                WSPasswordCallback pc = (WSPasswordCallback) callbacks[0];
                if (pc.getIdentifier().equals(username))
                {
                    pc.setPassword(password);
                }
            }
        });

        if (builder.getOutInterceptors() == null)
        {
            builder.setOutInterceptors(new ArrayList<Interceptor<? extends Message>>());
        }

        builder.getOutInterceptors().add(new WSS4JOutInterceptor(configProperties));
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

    public PasswordType getPasswordType()
    {
        return passwordType;
    }

    public void setPasswordType(PasswordType passwordType)
    {
        this.passwordType = passwordType;
    }

    public boolean isAddNonce()
    {
        return addNonce;
    }

    public void setAddNonce(boolean addNonce)
    {
        this.addNonce = addNonce;
    }

    public boolean isAddCreated()
    {
        return addCreated;
    }

    public void setAddCreated(boolean addCreated)
    {
        this.addCreated = addCreated;
    }
}
