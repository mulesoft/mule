/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.sftp;

import static org.junit.Assert.assertEquals;

import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class PreferredAuthenticationConfigTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "preferred-authentication-config.xml";
    }

    @Test
    public void usesDefaultPreferredAuthenticationMethodsProperty() throws Exception
    {
        SftpConnector defaultSftpConnector = (SftpConnector) muleContext.getRegistry().lookupConnector("default");
        assertEquals(null, defaultSftpConnector.getPreferredAuthenticationMethods());
    }

    @Test
    public void setsPreferredAuthenticationMethodsProperty() throws Exception
    {
        SftpConnector customizedConnector = (SftpConnector) muleContext.getRegistry().lookupConnector("customized");
        assertEquals("password", customizedConnector.getPreferredAuthenticationMethods());
    }
}
