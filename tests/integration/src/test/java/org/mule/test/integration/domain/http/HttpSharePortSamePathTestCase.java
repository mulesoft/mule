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
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transport.ConnectorException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.ApplicationContextBuilder;
import org.mule.tck.junit4.DomainContextBuilder;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

import java.util.Arrays;
import java.util.Collection;

import org.hamcrest.core.IsInstanceOf;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class HttpSharePortSamePathTestCase extends AbstractMuleTestCase
{

    private final String domainConfig;
    private final String appConfig;
    private final Class expectedExceptionType;

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");
    @Rule
    public SystemProperty endpointScheme = getEndpointSchemeSystemProperty();

    public HttpSharePortSamePathTestCase(String domainConfig, String appConfig, Class expectedExceptionType)
    {
        this.domainConfig = domainConfig;
        this.appConfig = appConfig;
        this.expectedExceptionType = expectedExceptionType;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {"domain/http/transport/http-shared-connector.xml", "domain/http/transport/http-hello-mule-app.xml", ConnectorException.class},
                {"domain/http/http-shared-listener-config.xml", "domain/http/http-hello-mule-app.xml", InitialisationException.class}});
    }

    @Test
    public void samePathDefinedInTwoAppsWithinSameDomain() throws Exception
    {
        MuleContext domainContext = new DomainContextBuilder().setDomainConfig(domainConfig).build();
        MuleContext firstAppContext = new ApplicationContextBuilder().setApplicationResources(new String[] {appConfig}).setDomainContext(domainContext).build();
        ApplicationContextBuilder secondApp = new ApplicationContextBuilder();
        try
        {
            secondApp.setApplicationResources(new String[] {appConfig}).setDomainContext(domainContext).build();
            fail("Second app context start should fail");
        }
        catch (Exception e)
        {
            assertThat(e.getCause(), IsInstanceOf.instanceOf(expectedExceptionType));
        }
        firstAppContext.dispose();
    }

    public SystemProperty getEndpointSchemeSystemProperty()
    {
        return new SystemProperty("scheme", "http");
    }
}
