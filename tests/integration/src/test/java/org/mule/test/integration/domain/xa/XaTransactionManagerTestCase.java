/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.domain.xa;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mule.api.config.ConfigurationException;
import org.mule.tck.junit4.DomainFunctionalTestCase;

public class XaTransactionManagerTestCase extends DomainFunctionalTestCase
{

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Override
    public void setUpMuleContexts() throws Exception
    {
        thrown.expect(ConfigurationException.class);
        super.setUpMuleContexts();
    }

    public static final String APPLICATION_NAME = "app";

    @Override
    protected String getDomainConfig()
    {
        return "domain/xa/jboss-ts-config.xml";
    }

    @Override
    public ApplicationConfig[] getConfigResources()
    {
        return new ApplicationConfig[] {
                new ApplicationConfig(APPLICATION_NAME, new String[] {"domain/xa/app-with-tx-manager-config.xml"})
        };
    }

    @Test
    public void validateOnlyOneTxManagerCanBeUsed()
    {
        // This is never called since the exception is thrown during init.
        getMuleContextForApp(APPLICATION_NAME).getTransactionManager();
    }

}
