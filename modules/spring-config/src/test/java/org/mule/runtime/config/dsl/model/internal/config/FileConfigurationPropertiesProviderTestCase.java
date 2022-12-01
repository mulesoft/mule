/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.dsl.model.internal.config;

import static org.mule.tck.junit4.matcher.IsEqualIgnoringLineBreaks.equalToIgnoringLineBreaks;
import static org.mule.test.allure.AllureConstants.ConfigurationProperties.CONFIGURATION_PROPERTIES;
import static org.mule.test.allure.AllureConstants.ConfigurationProperties.ComponentConfigurationAttributesStory.CONFIGURATION_PROPERTIES_RESOLVER_STORY;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;

import org.mule.runtime.config.api.dsl.model.ResourceProvider;
import org.mule.runtime.config.api.properties.ConfigurationPropertiesHierarchyBuilder;
import org.mule.runtime.config.api.properties.ConfigurationPropertiesResolver;
import org.mule.runtime.config.internal.dsl.model.ClassLoaderResourceProvider;
import org.mule.tck.junit4.AbstractMuleTestCase;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@Feature(CONFIGURATION_PROPERTIES)
@Story(CONFIGURATION_PROPERTIES_RESOLVER_STORY)
public class FileConfigurationPropertiesProviderTestCase extends AbstractMuleTestCase {

  private ConfigurationPropertiesResolver resolver;

  @Rule
  public ExpectedException expectedException = none();

  @Before
  public void createResolver() {
    ResourceProvider externalResourceProvider = new ClassLoaderResourceProvider(Thread.currentThread().getContextClassLoader());
    resolver = new ConfigurationPropertiesHierarchyBuilder().withPropertiesFile(externalResourceProvider).build();
  }

  @Test
  public void fileIsResolved() {
    assertThat((String) resolver.resolveValue("${file::dummy.xml}"),
               is(equalToIgnoringLineBreaks("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<foo/>\n")));
  }

  @Test
  public void fileDoesNotExist() {
    expectedException.expectMessage(is("Couldn't find configuration property value for key ${file::non-existing-file}"));
    resolver.resolveValue("${file::non-existing-file}");
  }
}
