/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.security;

import static org.apache.ws.security.WSConstants.CREATED_LN;
import static org.apache.ws.security.WSConstants.NONCE_LN;
import static org.apache.ws.security.WSPasswordCallback.USERNAME_TOKEN;
import static org.apache.ws.security.handler.WSHandlerConstants.ADD_UT_ELEMENTS;
import static org.apache.ws.security.handler.WSHandlerConstants.PASSWORD_TYPE;
import static org.apache.ws.security.handler.WSHandlerConstants.PW_CALLBACK_REF;
import static org.apache.ws.security.handler.WSHandlerConstants.USER;
import static org.junit.Assert.assertEquals;
import static org.mule.module.ws.security.PasswordType.TEXT;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.HashMap;
import java.util.Map;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;

import org.apache.ws.security.WSPasswordCallback;
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
        strategy.setPasswordType(TEXT);
        strategy.apply(properties, null);

        assertEquals(username, properties.get(USER));
        assertEquals("PasswordText", properties.get(PASSWORD_TYPE));

        CallbackHandler handler = (CallbackHandler) properties.get(PW_CALLBACK_REF);
        WSPasswordCallback passwordCallback = new WSPasswordCallback(username, USERNAME_TOKEN);
        handler.handle(new Callback[] { passwordCallback });

        assertEquals(password, passwordCallback.getPassword());
    }

    @Test
    public void nonceAndCreatedAreSetInTheMap()
    {
        Map<String, Object> properties = new HashMap<String, Object>();

        strategy.setAddNonce(true);
        strategy.setAddCreated(true);
        strategy.apply(properties, null);

        assertEquals(NONCE_LN + " " + CREATED_LN, properties.get(ADD_UT_ELEMENTS));
    }

}
