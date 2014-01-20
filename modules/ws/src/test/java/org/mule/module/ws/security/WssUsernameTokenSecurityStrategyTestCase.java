/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.security;

import static org.junit.Assert.assertEquals;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.HashMap;
import java.util.Map;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;

import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSPasswordCallback;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.junit.Test;

@SmallTest
public class WssUsernameTokenSecurityStrategyTestCase extends AbstractMuleTestCase
{

    private WssUsernameTokenSecurityStrategy strategy = new WssUsernameTokenSecurityStrategy();

    @Test
    public void usernameTokenPropertiesAreSetInTheMap() throws Exception
    {
        Map<String, Object> properties = new HashMap<String, Object>();

        String username = "testUsername";
        String password = "testPassword";

        strategy.setUsername(username);
        strategy.setPassword(password);
        strategy.setPasswordType(PasswordType.TEXT);
        strategy.apply(properties);

        assertEquals(username, properties.get(WSHandlerConstants.USER));
        assertEquals("PasswordText", properties.get(WSHandlerConstants.PASSWORD_TYPE));

        CallbackHandler handler = (CallbackHandler) properties.get(WSHandlerConstants.PW_CALLBACK_REF);
        WSPasswordCallback passwordCallback = new WSPasswordCallback(username, 1);
        handler.handle(new Callback[] { passwordCallback });

        assertEquals(password, passwordCallback.getPassword());
    }

    @Test
    public void nonceAndCreatedAreSetInTheMap()
    {
        Map<String, Object> properties = new HashMap<String, Object>();

        strategy.setAddNonce(true);
        strategy.setAddCreated(true);
        strategy.apply(properties);

        assertEquals(WSConstants.NONCE_LN + " " + WSConstants.CREATED_LN, properties.get(WSHandlerConstants.ADD_UT_ELEMENTS));
    }

}
