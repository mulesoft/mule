/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config;

import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_CONFIGURATION;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_TIME_SUPPLIER;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.test.allure.AllureConstants.ConfigurationProperties.CONFIGURATION_PROPERTIES;
import static org.mule.test.allure.AllureConstants.ConfigurationProperties.ComponentConfigurationAttributesStory.COMPONENT_CONFIGURATION_PROPERTIES_STORY;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.concurrent.TimeUnit.DAYS;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.config.custom.ServiceConfigurator;
import org.mule.runtime.api.time.TimeSupplier;
import org.mule.runtime.config.internal.SpringXmlConfigurationBuilder;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.context.DefaultMuleContextFactory;
import org.mule.runtime.core.internal.config.ImmutableExpirationPolicy;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.transformer.simple.ObjectToInputStream;
import org.mule.runtime.extension.api.runtime.ExpirationPolicy;
import org.mule.tck.config.TestServicesConfigurationBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.MockExtensionManagerConfigurationBuilder;

import java.util.Calendar;

import org.slf4j.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(CONFIGURATION_PROPERTIES)
@Story(COMPONENT_CONFIGURATION_PROPERTIES_STORY)
public class MuleConfigurationConfiguratorTestCase extends AbstractMuleTestCase {

  private static final Logger LOGGER = getLogger(MuleConfigurationConfiguratorTestCase.class);

  @Rule
  public TestServicesConfigurationBuilder testServicesConfigurationBuilder = new TestServicesConfigurationBuilder();

  private final TimeSupplier timeSupplier = mock(TimeSupplier.class);

  private MuleContextWithRegistry muleContext;

  @Before
  public void before() throws Exception {
    muleContext = (MuleContextWithRegistry) new DefaultMuleContextFactory()
        .createMuleContext(testServicesConfigurationBuilder,
                           new ConfigurationBuilder() {

                             @Override
                             public void configure(MuleContext muleContext) throws ConfigurationException {
                               muleContext.getCustomizationService().overrideDefaultServiceImpl(OBJECT_TIME_SUPPLIER,
                                                                                                timeSupplier);
                             }

                             @Override
                             public void addServiceConfigurator(ServiceConfigurator serviceConfigurator) {}
                           },
                           new MockExtensionManagerConfigurationBuilder(),
                           new SpringXmlConfigurationBuilder(new String[0], emptyMap()));
    muleContext.start();
    muleContext.getRegistry().lookupByType(Calendar.class);
  }

  @After
  public void after() {
    if (muleContext != null) {
      disposeIfNeeded(muleContext, LOGGER);
    }
  }

  @Test
  @Issue("MULE-19006")
  public void configuratorExpirationPolicyUsesManagedTimeSupplier() throws Exception {
    MuleConfiguration configuration = muleContext.getRegistry()
        .lookupObject(OBJECT_MULE_CONFIGURATION);
    ExpirationPolicy policy = configuration.getDynamicConfigExpiration().getExpirationPolicy();
    assertThat(policy, instanceOf(ImmutableExpirationPolicy.class));
    // This is done so that the timeSupplier is invoked.
    configuration.getDynamicConfigExpiration().getExpirationPolicy().isExpired(0L, DAYS);
    verify(timeSupplier).getAsLong();
  }

  @Test
  @Issue("MULE-20031")
  public void muleContextInjectedIntoTransformers() throws Exception {
    ObjectToInputStream o2isTransformer = muleContext.getRegistry().lookupObject(ObjectToInputStream.class);

    assertThat(o2isTransformer.doTransform(singletonMap("key", "value"), UTF_8), not(nullValue()));
  }
}
