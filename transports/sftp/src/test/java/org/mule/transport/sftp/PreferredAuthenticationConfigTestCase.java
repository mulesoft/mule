/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.sftp;

import static org.junit.Assert.assertEquals;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class PreferredAuthenticationConfigTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
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
