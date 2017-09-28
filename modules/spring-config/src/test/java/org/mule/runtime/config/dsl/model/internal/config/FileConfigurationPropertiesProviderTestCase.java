/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.dsl.model.internal.config;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mule.test.allure.AllureConstants.ConfigurationProperties.CONFIGURATION_PROPERTIES;
import static org.mule.test.allure.AllureConstants.ConfigurationProperties.ComponentConfigurationAttributesStory.CONFIGURATION_PROPERTIES_RESOLVER_STORY;

import org.mule.runtime.config.api.dsl.model.ResourceProvider;
import org.mule.runtime.config.internal.dsl.model.ClassLoaderResourceProvider;
import org.mule.runtime.config.internal.dsl.model.config.DefaultConfigurationPropertiesResolver;
import org.mule.runtime.config.internal.dsl.model.config.FileConfigurationPropertiesProvider;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Optional;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@Feature(CONFIGURATION_PROPERTIES)
@Story(CONFIGURATION_PROPERTIES_RESOLVER_STORY)
public class FileConfigurationPropertiesProviderTestCase extends AbstractMuleTestCase {

  private DefaultConfigurationPropertiesResolver resolver;

  @Rule
  public ExpectedException expectedException = none();

  @Before
  public void createResolver() {
    ResourceProvider externalResourceProvider = new ClassLoaderResourceProvider(Thread.currentThread().getContextClassLoader());
    resolver = new DefaultConfigurationPropertiesResolver(Optional
        .empty(), new FileConfigurationPropertiesProvider(externalResourceProvider, "External provider"));
  }

  @Test
  public void fileIsResolved() {
    assertThat(resolver.resolveValue("${file::dummy.xml}"), is("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<foo/>\n"));
  }

  @Test
  public void fileDoesNotExist() {
    expectedException
        .expectMessage(is("Couldn't find configuration property value for key ${file::non-existing-file} from properties provider External provider"));
    resolver.resolveValue("${file::non-existing-file}");
  }
}
