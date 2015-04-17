/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.ws.security;

import static org.apache.ws.security.WSConstants.CREATED_LN;
import static org.apache.ws.security.WSConstants.NONCE_LN;
import static org.apache.ws.security.handler.WSHandlerConstants.ADD_UT_ELEMENTS;
import static org.apache.ws.security.handler.WSHandlerConstants.PASSWORD_TYPE;
import static org.apache.ws.security.handler.WSHandlerConstants.USER;
import static org.apache.ws.security.handler.WSHandlerConstants.USERNAME_TOKEN;
import static org.mule.module.ws.security.PasswordType.DIGEST;
import static org.mule.module.ws.security.PasswordType.TEXT;
import org.mule.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.ws.security.WSPasswordCallback;

public class WssUsernameTokenSecurityStrategy extends AbstractSecurityStrategy implements SecurityStrategy
{

    private String username;
    private String password;
    private PasswordType passwordType;
    private boolean addNonce;
    private boolean addCreated;

    @Override
    public void apply(Map<String, Object> outConfigProperties, Map<String, Object> inConfigProperties)
    {
        appendAction(outConfigProperties, USERNAME_TOKEN);
        outConfigProperties.put(USER, username);

        if (passwordType == TEXT)
        {
            outConfigProperties.put(PASSWORD_TYPE, "PasswordText");
        }
        else if (passwordType == DIGEST)
        {
            outConfigProperties.put(PASSWORD_TYPE, "PasswordDigest");
        }

        List<String> additionalElements = new ArrayList<String>(2);
        if (addNonce)
        {
            additionalElements.add(NONCE_LN);
        }
        if (addCreated)
        {
            additionalElements.add(CREATED_LN);
        }
        if (!additionalElements.isEmpty())
        {
            outConfigProperties.put(ADD_UT_ELEMENTS, StringUtils.join(additionalElements, " "));
        }

        addPasswordCallbackHandler(outConfigProperties, new WSPasswordCallbackHandler(WSPasswordCallback.USERNAME_TOKEN)
        {
            @Override
            public void handle(WSPasswordCallback passwordCallback)
            {
                if (passwordCallback.getIdentifier().equals(username))
                {
                    passwordCallback.setPassword(password);
                }
            }
        });
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
