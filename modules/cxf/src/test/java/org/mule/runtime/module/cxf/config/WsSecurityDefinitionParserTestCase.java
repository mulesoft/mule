/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.cxf.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.module.cxf.wssec.ClientPasswordCallback;

import java.util.Map;

import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.validate.NoOpValidator;
import org.junit.Test;

public class WsSecurityDefinitionParserTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "ws-security-config.xml";
    }

    @Test
    public void testWsSecurityConfig()
    {
        WsSecurity wsSecurity = muleContext.getRegistry().lookupObject("config1");
        assertNotNull(wsSecurity);

        assertNotNull(wsSecurity.getSecurityManager());
        assertNotNull(wsSecurity.getConfigProperties());
        assertFalse(wsSecurity.getConfigProperties().isEmpty());
        Map<String, Object> wsProperties = wsSecurity.getConfigProperties();
        assertEquals(WSHandlerConstants.USERNAME_TOKEN, wsProperties.get(WSHandlerConstants.ACTION));
        assertEquals("joe", wsProperties.get(WSHandlerConstants.USER));
        assertEquals("PasswordText", wsProperties.get(WSHandlerConstants.PASSWORD_TYPE));
        assertEquals("org.mule.runtime.module.cxf.wssec.ClientPasswordCallback", wsProperties.get(WSHandlerConstants.PW_CALLBACK_CLASS));

        assertNotNull(wsSecurity.getCustomValidator());
        assertFalse(wsSecurity.getCustomValidator().isEmpty());

        assertTrue(wsSecurity.getCustomValidator().get(SecurityConstants.USERNAME_TOKEN_VALIDATOR) instanceof NoOpValidator);

    }

    @Test
    public void testWsSecurityConfig2()
    {
        WsSecurity wsSecurity = muleContext.getRegistry().lookupObject("config2");
        assertNotNull(wsSecurity);

        assertNotNull(wsSecurity.getConfigProperties());
        assertFalse(wsSecurity.getConfigProperties().isEmpty());
        Map<String, Object> wsProperties = wsSecurity.getConfigProperties();
        
        assertEquals(WSHandlerConstants.TIMESTAMP + " " + WSHandlerConstants.SIGNATURE + " "
                + WSHandlerConstants.ENCRYPT, wsProperties.get(WSHandlerConstants.ACTION));
        assertEquals("joe", wsProperties.get(WSHandlerConstants.USER));
        assertEquals("org/mule/runtime/module/cxf/wssec/wssecurity.properties", wsProperties.get(WSHandlerConstants.SIG_PROP_FILE));
        assertEquals("org/mule/runtime/module/cxf/wssec/wssecurity.properties", wsProperties.get(WSHandlerConstants.ENC_PROP_FILE));
        assertTrue(wsProperties.get(WSHandlerConstants.PW_CALLBACK_REF) instanceof ClientPasswordCallback);
    }

    @Test
    public void testWsSecurityConfigCustomTimestampValidator()
    {
        WsSecurity wsSecurity = muleContext.getRegistry().lookupObject("customTimestampConfig");
        assertNotNull(wsSecurity);

        assertNotNull(wsSecurity.getCustomValidator());
        assertFalse(wsSecurity.getCustomValidator().isEmpty());

        assertTrue(wsSecurity.getCustomValidator().get(SecurityConstants.TIMESTAMP_TOKEN_VALIDATOR) instanceof NoOpValidator);
    }

    @Test
    public void testWsSecurityConfigCustomSAML1Validator()
    {
        WsSecurity wsSecurity = muleContext.getRegistry().lookupObject("customSAML1Config");
        assertNotNull(wsSecurity);

        assertNotNull(wsSecurity.getCustomValidator());
        assertFalse(wsSecurity.getCustomValidator().isEmpty());

        assertTrue(wsSecurity.getCustomValidator().get(SecurityConstants.SAML1_TOKEN_VALIDATOR) instanceof NoOpValidator);
    }

    @Test
    public void testWsSecurityConfigCustomSAML2Validator()
    {
        WsSecurity wsSecurity = muleContext.getRegistry().lookupObject("customSAML2Config");
        assertNotNull(wsSecurity);

        assertNotNull(wsSecurity.getCustomValidator());
        assertFalse(wsSecurity.getCustomValidator().isEmpty());

        assertTrue(wsSecurity.getCustomValidator().get(SecurityConstants.SAML2_TOKEN_VALIDATOR) instanceof NoOpValidator);
    }

    @Test
    public void testWsSecurityConfigCustomSignatureValidator()
    {
        WsSecurity wsSecurity = muleContext.getRegistry().lookupObject("customSignatureConfig");
        assertNotNull(wsSecurity);

        assertNotNull(wsSecurity.getCustomValidator());
        assertFalse(wsSecurity.getCustomValidator().isEmpty());

        assertTrue(wsSecurity.getCustomValidator().get(SecurityConstants.SIGNATURE_TOKEN_VALIDATOR) instanceof NoOpValidator);
    }

    @Test
    public void testWsSecurityConfigCustomBSTValidator()
    {
        WsSecurity wsSecurity = muleContext.getRegistry().lookupObject("customBSTConfig");
        assertNotNull(wsSecurity);

        assertNotNull(wsSecurity.getCustomValidator());
        assertFalse(wsSecurity.getCustomValidator().isEmpty());

        assertTrue(wsSecurity.getCustomValidator().get(SecurityConstants.BST_TOKEN_VALIDATOR) instanceof NoOpValidator);
    }

    @Test
    public void testWsSecurityConfigCustomMultipleValidator()
    {
        WsSecurity wsSecurity = muleContext.getRegistry().lookupObject("customMultipleConfig");
        assertNotNull(wsSecurity);

        assertNotNull(wsSecurity.getCustomValidator());
        assertFalse(wsSecurity.getCustomValidator().isEmpty());

        assertTrue(wsSecurity.getCustomValidator().get(SecurityConstants.USERNAME_TOKEN_VALIDATOR) instanceof NoOpValidator);
        assertTrue(wsSecurity.getCustomValidator().get(SecurityConstants.SAML1_TOKEN_VALIDATOR) instanceof NoOpValidator);
        assertTrue(wsSecurity.getCustomValidator().get(SecurityConstants.SAML2_TOKEN_VALIDATOR) instanceof NoOpValidator);
        assertTrue(wsSecurity.getCustomValidator().get(SecurityConstants.TIMESTAMP_TOKEN_VALIDATOR) instanceof NoOpValidator);
        assertTrue(wsSecurity.getCustomValidator().get(SecurityConstants.SIGNATURE_TOKEN_VALIDATOR) instanceof NoOpValidator);
        assertTrue(wsSecurity.getCustomValidator().get(SecurityConstants.BST_TOKEN_VALIDATOR) instanceof NoOpValidator);
    }

}
