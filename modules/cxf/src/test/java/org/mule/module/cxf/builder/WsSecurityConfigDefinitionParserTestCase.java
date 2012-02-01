/*
 * $Id $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.cxf.builder;

import static org.junit.Assert.assertEquals;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.mule.module.cxf.config.WsSecurityConfigDefinitionParser;

import org.apache.ws.security.handler.WSHandlerConstants;
import org.junit.Before;
import org.junit.Test;

@SmallTest
public class WsSecurityConfigDefinitionParserTestCase extends AbstractMuleTestCase
{
    private WsSecurityConfigDefinitionParser wsSecurityConfigDefinitionParser;

    @Before
    public void setUp()
    {
        wsSecurityConfigDefinitionParser = new WsSecurityConfigDefinitionParser("configProperties");
    }

    @Test
    public void testParseKeyNameAction()
    {
        assertEquals(WSHandlerConstants.ACTION, wsSecurityConfigDefinitionParser.parseKeyName("action"));
    }

    @Test
    public void testParseKeyNameCallbackHandlerClass()
    {
        assertEquals(WSHandlerConstants.PW_CALLBACK_CLASS, wsSecurityConfigDefinitionParser.parseKeyName("password-callback-class"));
    }

    @Test
    public void testParseKeyNameCallbackPropFile()
    {
        assertEquals(WSHandlerConstants.DEC_PROP_FILE, wsSecurityConfigDefinitionParser.parseKeyName("decryption-prop-file"));
    }

}
