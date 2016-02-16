/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.domain.xa;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.functional.junit4.ApplicationContextBuilder;
import org.mule.functional.junit4.DomainFunctionalTestCase;

import org.junit.Test;

public class XaTransactionManagerTestCase extends DomainFunctionalTestCase
{

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

    @Test(expected = MuleRuntimeException.class)
    public void validateOnlyOneTxManagerCanBeUsed() throws MuleException
    {
        getMuleContextForApp(APPLICATION_NAME).start();
    }

    @Override
    protected MuleContext createAppMuleContext(String[] configResource) throws Exception
    {
        ApplicationContextBuilder appContextBuilder = new ApplicationContextBuilder()
        {
            @Override
            public MuleContext build() throws Exception
            {
                // Do not start the context for this test.
                return doBuildContext();
            }
        }.setDomainContext(getMuleContextForDomain()).setApplicationResources(configResource);
        return appContextBuilder.build();
    }
}
