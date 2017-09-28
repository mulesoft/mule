/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.dsl.model.internal.config;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mule.test.allure.AllureConstants.ConfigurationProperties.CONFIGURATION_PROPERTIES;
import static org.mule.test.allure.AllureConstants.ConfigurationProperties.ComponentConfigurationAttributesStory.CONFIGURATION_PROPERTIES_RESOLVER_STORY;

import org.mule.runtime.config.internal.dsl.model.config.ConfigurationPropertiesProvider;
import org.mule.runtime.config.internal.dsl.model.config.ConfigurationProperty;
import org.mule.runtime.config.internal.dsl.model.config.DefaultConfigurationPropertiesResolver;
import org.mule.tck.junit4.AbstractMuleTestCase;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Optional;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@Feature(CONFIGURATION_PROPERTIES)
@Story(CONFIGURATION_PROPERTIES_RESOLVER_STORY)
public class PropertiesResolverConfigurationPropertiesResolverTestCase extends AbstractMuleTestCase {

  private static final String FIXED_VALUE = "fixedValue";
  private static final String CHILD_RESOLVER_DESCRIPTION = "child resolver";
  private static final String PARENT_RESOLVER_DESCRIPTION = "parent resolver";
  private DefaultConfigurationPropertiesResolver resolver;

  @Rule
  public ExpectedException expectedException = none();

  @Before
  public void createResolver() {
    DefaultConfigurationPropertiesResolver parentResolver =
        new DefaultConfigurationPropertiesResolver(Optional.empty(), new ConfigurationPropertiesProvider() {

          private List<ConfigurationProperty> attributes = ImmutableList.<ConfigurationProperty>builder()
              .add(new ConfigurationProperty(this, "parent-key1", "parent-value1"))
              .add(new ConfigurationProperty(this, "parent-key2", "parent-value2"))
              .add(new ConfigurationProperty(this, "parent-complex-key1", "parent-complex-${parent-key1}"))
              .add(new ConfigurationProperty(this, "parent-complex-key2", "${parent-key1}-${parent-complex-key3}"))
              .add(new ConfigurationProperty(this, "parent-complex-key3", "${parent-key2}"))
              .add(new ConfigurationProperty(this, "parent-key-referencing-child", "${child-key1}"))
              .build();

          @Override
          public Optional<ConfigurationProperty> getConfigurationProperty(String configurationAttributeKey) {
            return attributes.stream().filter(cf -> cf.getKey().equals(configurationAttributeKey)).findFirst();
          }

          @Override
          public String getDescription() {
            return PARENT_RESOLVER_DESCRIPTION;
          }
        });
    resolver = new DefaultConfigurationPropertiesResolver(Optional.of(parentResolver), new ConfigurationPropertiesProvider() {

      private List<ConfigurationProperty> attributes = ImmutableList.<ConfigurationProperty>builder()
          .add(new ConfigurationProperty(this, "child-key1", "child-value1"))
          .add(new ConfigurationProperty(this, "child-key2", "child-value2"))
          .add(new ConfigurationProperty(this, "child-complex-key1", "${child-key1}-${parent-complex-key1}"))
          .add(new ConfigurationProperty(this, "child-complex-key2", "${child-key1}-${parent-complex-key2}-${child-key2}"))
          .add(new ConfigurationProperty(this, "unresolved-nested-key", "${child-key1}-${child-key3}"))
          .add(new ConfigurationProperty(this, "invalid-key1", "${nonExistentKey}"))
          .build();

      @Override
      public Optional<ConfigurationProperty> getConfigurationProperty(String configurationAttributeKey) {
        return attributes.stream().filter(cf -> cf.getKey().equals(configurationAttributeKey)).findFirst();
      }

      @Override
      public String getDescription() {
        return CHILD_RESOLVER_DESCRIPTION;
      }
    });
  }

  @Test
  public void resolveNoPlaceholder() {
    assertThat(resolver.resolveValue(FIXED_VALUE), is(FIXED_VALUE));
  }

  @Test
  public void nullValueReturnsNull() {
    assertThat(resolver.resolveValue(null), nullValue());
  }

  @Test
  public void resolveKeyInParent() {
    assertThat(resolver.resolveValue("${parent-key1}"), is("parent-value1"));
  }

  @Test
  public void resolveKeyInChild() {
    assertThat(resolver.resolveValue("${child-key1}"), is("child-value1"));
  }

  @Test
  public void resolveParentComplexKey() {
    assertThat(resolver.resolveValue("${parent-complex-key1}"), is("parent-complex-parent-value1"));
  }

  @Test
  public void resolveChildComplexKey() {
    assertThat(resolver.resolveValue("${child-complex-key1}"), is("child-value1-parent-complex-parent-value1"));
  }

  @Test
  public void resolveKeyWithServeralLevelsOfIndirection() {
    assertThat(resolver.resolveValue("${child-complex-key2}"), is("child-value1-parent-value1-parent-value2-child-value2"));
  }

  @Test
  public void parentKeyCannotReferenceChildKey() {
    expectedException
        .expectMessage(is("Couldn't find configuration property value for key ${child-key1} from properties provider parent resolver - within resolver child resolver trying to process key parent-key-referencing-child"));
    resolver.resolveValue("${parent-key-referencing-child}");
  }

  @Test
  public void resolveInvalidKey() {
    expectedException
        .expectMessage(is("Couldn't find configuration property value for key ${nonExistentKey} from properties provider parent resolver - within resolver child resolver trying to process key nonExistentKey"));
    resolver.resolveValue("${invalid-key1}");
  }

  @Test
  public void unresolvedNestedKey() {
    expectedException
        .expectMessage(is("Couldn't find configuration property value for key ${child-key3} from properties provider parent resolver - within resolver child resolver trying to process key child-key3"));
    resolver.resolveValue("${unresolved-nested-key}");
  }

}
