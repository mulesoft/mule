/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.domain.lifecycle;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.transport.Connector;
import org.mule.config.bootstrap.MuleRegistryBootstrapService;
import org.mule.config.bootstrap.RegistryBootstrapService;
import org.mule.config.bootstrap.RegistryBootstrapServiceUtil;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.ApplicationContextBuilder;
import org.mule.tck.junit4.DomainContextBuilder;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.util.IOUtils;

import org.hamcrest.core.Is;
import org.junit.Rule;
import org.junit.Test;

public class  AppAndDomainLifecycleTestCase extends AbstractMuleTestCase
{

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");
    @Rule
    public SystemProperty endpointScheme = getEndpointSchemeSystemProperty();

    @Test
    public void appShutdownDoesNotStopsDomainConnector() throws Exception
    {
        RegistryBootstrapService registryBootstrapService = new MuleRegistryBootstrapService();
        RegistryBootstrapServiceUtil.configureUsingClassPath(registryBootstrapService);

        MuleContext domainContext = null;
        MuleContext firstAppContext = null;
        MuleContext secondAppContext = null;
        try
        {
            domainContext = new DomainContextBuilder().setRegistryBootstrapService(registryBootstrapService).setDomainConfig("domain/http/transport/http-shared-connector.xml").build();
            firstAppContext = new ApplicationContextBuilder().setRegistryBootstrapService(registryBootstrapService).setApplicationResources(new String[] {"domain/http/transport/http-hello-mule-app.xml"}).setDomainContext(domainContext).build();
            ApplicationContextBuilder secondApp = new ApplicationContextBuilder().setRegistryBootstrapService(registryBootstrapService);
            secondAppContext = secondApp.setApplicationResources(new String[] {"domain/http/transport/http-hello-world-app.xml"}).setDomainContext(domainContext).build();
            firstAppContext.stop();
            MuleMessage response = secondAppContext.getClient().send("http://localhost:" + dynamicPort.getNumber() + "/service/helloWorld", new DefaultMuleMessage("test", firstAppContext));
            assertThat(response, notNullValue());
            assertThat(response.getPayloadAsString(), is("hello world"));
            assertThat((domainContext.getRegistry().<Connector>get("sharedHttpConnector")).isStarted(), Is.is(true));
        }
        finally
        {
            closeQuietly(domainContext);
            closeQuietly(firstAppContext);
            closeQuietly(secondAppContext);
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

    public SystemProperty getEndpointSchemeSystemProperty()
    {
        return new SystemProperty("scheme", "http");
    }

}
