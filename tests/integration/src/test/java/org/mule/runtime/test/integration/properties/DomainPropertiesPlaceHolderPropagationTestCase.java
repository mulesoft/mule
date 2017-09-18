/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.test.integration.properties;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.functional.junit4.ApplicationContextBuilder;
import org.mule.functional.junit4.DomainContextBuilder;
import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.After;
import org.junit.Test;

public class DomainPropertiesPlaceHolderPropagationTestCase extends AbstractMuleTestCase {

  private MuleContext domainContext;
  private MuleContext applicationContext;

  @Test
  public void propertiesPropagatesToAppUsingContext() throws Exception {
    configureContexts("properties/domain/shared-context-properties.xml", "properties/domain/app-with-no-properties.xml");
    propertiesPropagatesScenario();
  }

  @Test
  public void appPropertiesPrecedeDomainPropertiesUsingContext() throws Exception {
    configureContexts("properties/domain/shared-context-properties.xml", "properties/domain/app-with-context-properties.xml");
    appPropertiesPrecedeDomainPropertiesScenario();
  }

  private void appPropertiesPrecedeDomainPropertiesScenario() throws RegistrationException {
    String domainPropertyObject = getDomainProperty("domainPropertyObject");
    assertThat(domainPropertyObject, is("9999"));
    String appPropertyObject = getApplicationProperty("appPropertyObject");
    assertThat(appPropertyObject, is("10000"));
    String app2PropertyObject = getApplicationProperty("app2PropertyObject");
    assertThat(app2PropertyObject, is("service"));
  }

  private void propertiesPropagatesScenario() throws RegistrationException {
    String domainPropertyObject = getDomainProperty("domainPropertyObject");
    assertThat(domainPropertyObject, is("9999"));
    String appPropertyObject = getApplicationProperty("appPropertyObject");
    assertThat(appPropertyObject, is("9999"));
    String inlinePropertyObject = getApplicationProperty("inlinePropertyObject");
    assertThat(inlinePropertyObject, is("file contents\n"));
  }

  private String getApplicationProperty(String property) throws RegistrationException {
    return applicationContext.getRegistry().lookupObject(ConfigurationProperties.class).resolveStringProperty(property).get();
  }

  private String getDomainProperty(String property) throws org.mule.runtime.core.api.registry.RegistrationException {
    return domainContext.getRegistry().lookupObject(ConfigurationProperties.class).resolveStringProperty(property).get();
  }

  private void configureContexts(String domainConfig, String appConfig) throws Exception {
    domainContext = new DomainContextBuilder().setDomainConfig(domainConfig).build();
    applicationContext =
        new ApplicationContextBuilder().setApplicationResources(new String[] {appConfig}).setDomainContext(domainContext).build();
  }

  @After
  public void after() {
    if (applicationContext != null) {
      applicationContext.dispose();
    }
    if (domainContext != null) {
      domainContext.dispose();
    }
  }
}
