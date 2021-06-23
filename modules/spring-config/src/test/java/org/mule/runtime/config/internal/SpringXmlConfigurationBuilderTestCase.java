/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import static org.hamcrest.Matchers.containsString;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.config.MuleRuntimeFeature.ENTITY_RESOLVER_FAIL_ON_FIRST_ERROR;
import static org.mule.tck.util.MuleContextUtils.mockContextWithServices;

import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.extension.api.dsl.syntax.resources.spi.ExtensionSchemaGenerator;

import java.util.HashMap;

import javax.inject.Inject;

import io.qameta.allure.Issue;
import org.hamcrest.CoreMatchers;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class SpringXmlConfigurationBuilderTestCase {

  @Inject
  private FeatureFlaggingService featureFlaggingService;

  private SpringXmlConfigurationBuilder configurationBuilder;
  private MuleContextWithRegistry muleContext;

  @Rule
  public ExpectedException expectedException = none();

  @Before
  public void setUp() throws Exception {
    muleContext = mockContextWithServices();
    muleContext.getInjector().inject(this);
    configurationBuilder =
        new SpringXmlConfigurationBuilder(new String[] {"invalid-schema.xml"}, new HashMap<>(), ArtifactType.APP, false, false);
  }

  @Test
  @Issue("EE-7827")
  public void configureWithFailOnFirstError() throws ConfigurationException {
    expectedException.expect(ConfigurationException.class);
    expectedException
        .expectMessage(containsString("Can't resolve http://www.mulesoft.org/schema/mule/invalid-namespace/current/invalid-schema.xsd, A dependency or plugin might be missing"));
    when(featureFlaggingService.isEnabled(ENTITY_RESOLVER_FAIL_ON_FIRST_ERROR)).thenReturn(true);

    configurationBuilder.configure(muleContext);
  }

  @Test
  @Issue("EE-7827")
  public void configureWithFailAfterTenErrors() throws ConfigurationException {
    Assume.assumeThat(System.getProperty("java.version"), CoreMatchers.startsWith("1.8"));
    expectedException.expect(ConfigurationException.class);
    expectedException
        .expectMessage(containsString("Invalid content was found starting with element 'invalid-namespace:config'."));
    when(featureFlaggingService.isEnabled(ENTITY_RESOLVER_FAIL_ON_FIRST_ERROR)).thenReturn(false);

    configurationBuilder.configure(muleContext);
  }

  public static final class TestExtensionSchemagenerator implements ExtensionSchemaGenerator {

    @Override
    public String generate(ExtensionModel extensionModel, DslResolvingContext context) {
      return "";
    }
  }

}
