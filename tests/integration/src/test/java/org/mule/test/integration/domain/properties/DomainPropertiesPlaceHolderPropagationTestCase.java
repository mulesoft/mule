/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.domain.properties;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.api.MuleContext;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.ApplicationContextBuilder;
import org.mule.tck.junit4.DomainContextBuilder;

import org.junit.Test;

public class DomainPropertiesPlaceHolderPropagationTestCase  extends AbstractMuleTestCase
{

    private MuleContext domainContext;
    private MuleContext applicationContext;

    @Test
    public void propertiesPropagatesToAppUsingContext() throws Exception
    {
        configureContexts("domain/properties/shared-context-properties.xml", "domain/properties/app-with-no-properties.xml");
        propertiesPropagatesScenario();
    }

    @Test
    public void appPropertiesPrecedeDomainPropertiesUsingContext() throws Exception
    {
        configureContexts("domain/properties/shared-context-properties.xml", "domain/properties/app-with-context-properties.xml");
        appPropertiesPrecedeDomainPropertiesScenario();
    }

    @Test
    public void propertiesPropagatesToAppUsingBeans() throws Exception
    {
        configureContexts("domain/properties/shared-beans-properties.xml", "domain/properties/app-with-no-properties.xml");
        propertiesPropagatesScenario();
    }

    @Test
    public void appPropertiesPrecedeDomainPropertiesUsingBeans() throws Exception
    {
        configureContexts("domain/properties/shared-beans-properties.xml", "domain/properties/app-with-beans-properties.xml");
        appPropertiesPrecedeDomainPropertiesScenario();
    }

    @Test
    public void propertiesPropagatesToAppUsingContextAndBeans() throws Exception
    {
        configureContexts("domain/properties/shared-context-properties.xml", "domain/properties/app-with-no-properties.xml");
        propertiesPropagatesScenario();
    }

    @Test
    public void appPropertiesPrecedeDomainPropertiesUsingContextAndBeans() throws Exception
    {
        configureContexts("domain/properties/shared-beans-properties.xml", "domain/properties/app-with-context-properties.xml");
        appPropertiesPrecedeDomainPropertiesScenario();
    }

    private void appPropertiesPrecedeDomainPropertiesScenario()
    {
        String domainPropertyObject = domainContext.getRegistry().lookupObject("domainPropertyObject");
        assertThat(domainPropertyObject, is("9999"));
        String appPropertyObject = applicationContext.getRegistry().lookupObject("appPropertyObject");
        assertThat(appPropertyObject, is("10000"));
        String app2PropertyObject = applicationContext.getRegistry().lookupObject("app2PropertyObject");
        assertThat(app2PropertyObject, is("service"));
    }

    private void propertiesPropagatesScenario()
    {
        String domainPropertyObject = domainContext.getRegistry().lookupObject("domainPropertyObject");
        assertThat(domainPropertyObject, is("9999"));
        String appPropertyObject = applicationContext.getRegistry().lookupObject("appPropertyObject");
        assertThat(appPropertyObject, is("9999"));
    }

    private void configureContexts(String domainConfig, String appConfig) throws Exception
    {
        domainContext = new DomainContextBuilder().setDomainConfig(domainConfig).build();
        applicationContext = new ApplicationContextBuilder().setApplicationResources(new String[] {appConfig}).setDomainContext(domainContext).build();
    }

}
