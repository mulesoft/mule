/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.domain.http.transport;

import org.mule.api.MuleContext;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.ApplicationContextBuilder;
import org.mule.tck.junit4.DomainContextBuilder;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class HttpDefaultConnectorTestCase extends AbstractMuleTestCase
{

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");


    @Test
    public void defaultHttpConnectorIsNotShared() throws Exception
    {
        MuleContext domainContext = null;
        MuleContext firstApp = null;
        MuleContext secondApp = null;
        try
        {
            domainContext = new DomainContextBuilder().setDomainConfig("domain/empty-domain-config.xml").build();
            firstApp = new ApplicationContextBuilder().setDomainContext(domainContext).setApplicationResources(new String[] {"domain/http/transport/http-hello-world-app-no-connector.xml"}).build();
            expectedException.expect(LifecycleException.class);
            secondApp = new ApplicationContextBuilder().setDomainContext(domainContext).setApplicationResources(new String[] {"domain/http/transport/http-hello-mule-app-no-connector.xml"}).build();
        }
        finally
        {
            closeQuietly(domainContext);
            closeQuietly(firstApp);
            closeQuietly(secondApp);
        }
    }

    private void closeQuietly(MuleContext context)
    {
        if (context != null)
        {
            try
            {
                context.dispose();
            }
            catch (Exception e)
            {
                //Do nothing
            }
        }
    }

}
