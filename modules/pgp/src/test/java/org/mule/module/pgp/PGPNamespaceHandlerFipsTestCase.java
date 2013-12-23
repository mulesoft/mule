/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.pgp;

import static org.junit.Assert.assertTrue;
import org.mule.api.MuleContext;
import org.mule.api.config.ConfigurationException;
import org.mule.api.config.MuleProperties;
import org.mule.api.security.tls.TlsConfiguration;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.ClassRule;
import org.junit.Test;

public class PGPNamespaceHandlerFipsTestCase extends FunctionalTestCase
{

    @ClassRule
    public static SystemProperty fipsSystemProperty = new SystemProperty(MuleProperties.MULE_SECURITY_SYSTEM_PROPERTY,
                                                                         TlsConfiguration.FIPS_SECURITY_MODEL);

    private boolean contextFailed = false;

    @Override
    protected String getConfigFile()
    {
        return "pgp-namespace-config.xml";
    }

    @Override
    protected MuleContext createMuleContext() throws Exception
    {
        MuleContext muleContext = null;

        try
        {
            muleContext = super.createMuleContext();
        }
        catch (ConfigurationException e)
        {
            contextFailed = true;
        }

        return muleContext;
    }

    @Test
    public void pgpElementsAreDisabledInFipsMode()
    {
        assertTrue(contextFailed);
    }
}
