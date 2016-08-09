/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.executor;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.extension.internal.util.ExtensionsTestUtils.getParameter;
import static org.mule.runtime.module.extension.internal.util.ExtensionsTestUtils.getResolver;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.parameter.ParameterModel;
import org.mule.runtime.extension.api.introspection.config.RuntimeConfigurationModel;
import org.mule.runtime.module.extension.internal.model.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.runtime.config.ConfigurationObjectBuilder;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Arrays;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ConfigurationObjectBuilderTestCase extends AbstractMuleTestCase {

  private static final String NAME_VALUE = "name";
  private static final String DESCRIPTION_VALUE = "description";

  public static final ParameterModel nameParameterModel = getParameter("name", String.class);
  public static final ParameterModel descriptionParameterModel = getParameter("description", String.class);

  public static ResolverSet createResolverSet() throws Exception {
    ResolverSet resolverSet = new ResolverSet();
    resolverSet.add(nameParameterModel.getName(), getResolver(NAME_VALUE));
    resolverSet.add(descriptionParameterModel.getName(), getResolver(DESCRIPTION_VALUE));

    return resolverSet;
  }

  @Mock
  private ExtensionModel extensionModel;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private RuntimeConfigurationModel configurationModel;

  @Mock
  private MuleEvent event;

  private TestConfig configuration;

  private ConfigurationObjectBuilder<TestConfig> configurationObjectBuilder;
  private ResolverSet resolverSet;


  @Before
  public void before() throws Exception {
    configuration = new TestConfig();

    when(configurationModel.getParameterModels()).thenReturn(Arrays.asList(nameParameterModel, descriptionParameterModel));
    when(configurationModel.getConfigurationFactory().newInstance()).thenReturn(configuration);
    when(configurationModel.getConfigurationFactory().getObjectType()).thenAnswer(invocation -> TestConfig.class);
    when(configurationModel.getModelProperty(ParameterGroupModelProperty.class)).thenReturn(Optional.empty());

    resolverSet = createResolverSet();

    configurationObjectBuilder = new ConfigurationObjectBuilder<>(configurationModel, resolverSet);
  }

  @Test
  public void build() throws Exception {
    TestConfig testConfig = configurationObjectBuilder.build(event);
    assertThat(testConfig.getName(), is(NAME_VALUE));
    assertThat(testConfig.getDescription(), is(DESCRIPTION_VALUE));
  }

  public static class TestConfig {

    private String name;
    private String description;

    public String getName() {
      return name;
    }

    public String getDescription() {
      return description;
    }
  }
}
