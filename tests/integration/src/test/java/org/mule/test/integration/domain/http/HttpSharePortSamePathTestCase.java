/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.domain.http;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.mule.api.MuleContext;
import org.mule.api.transport.ConnectorException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.ApplicationContextBuilder;
import org.mule.tck.junit4.DomainContextBuilder;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

import org.hamcrest.core.IsInstanceOf;
import org.junit.Rule;
import org.junit.Test;

public class HttpSharePortSamePathTestCase extends AbstractMuleTestCase
{

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");
    @Rule
    public SystemProperty endpointScheme = getEndpointSchemeSystemProperty();

    @Test
    public void samePathDefinedInTwoAppsWithinSameDomain() throws Exception
    {
        MuleContext domainContext = new DomainContextBuilder().setDomainConfig("domain/http/http-shared-connector.xml").build();
        MuleContext firstAppContext = new ApplicationContextBuilder().setApplicationResources(new String[] {"domain/http/http-hello-mule-app.xml"}).setDomainContext(domainContext).build();
        ApplicationContextBuilder secondApp = new ApplicationContextBuilder();
        try
        {
            secondApp.setApplicationResources(new String[] {"domain/http/http-hello-mule-app.xml"}).setDomainContext(domainContext).build();
            fail("Second app context start should fail");
        }
        catch (Exception e)
        {
            assertThat(e.getCause(), IsInstanceOf.instanceOf(ConnectorException.class));
        }
        firstAppContext.dispose();
    }

    public SystemProperty getEndpointSchemeSystemProperty()
    {
        return new SystemProperty("scheme", "http");
    }
}
