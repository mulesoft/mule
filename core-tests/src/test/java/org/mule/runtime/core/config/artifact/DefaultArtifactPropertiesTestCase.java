/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.config.artifact;

import static java.util.Collections.emptyMap;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.tck.MuleTestUtils.testWithSystemProperty;
import org.mule.runtime.core.internal.config.artifact.DefaultArtifactProperties;
import org.mule.tck.junit4.AbstractMuleTestCase;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

import org.junit.Test;

public class DefaultArtifactPropertiesTestCase extends AbstractMuleTestCase {

  private static final String OVERRIDE_PROPERTY_KEY = "overrideProperty";
  private static final String CONFIG_PROPERTY_OVERRIDE_VALUE = "configPropertyValue";
  private static final String ARTIFACT_PROPERTY_OVERRIDE_VALUE = "artifactPropertyValue";
  private static final String SPRING_PROPERTY_OVERRIDE_VALUE = "springPropertyValue";
  private static final String SYSTEM_PROPERTY_OVERRIDE_VALUE = "systemPropertyValue";

  private Map<Object, Object> configPropertiesOverriddenPropertyMap =
      ImmutableMap.builder().put(OVERRIDE_PROPERTY_KEY, CONFIG_PROPERTY_OVERRIDE_VALUE).build();
  private Map<Object, Object> artifactPropertiesOverriddenMap =
      ImmutableMap.builder().put(OVERRIDE_PROPERTY_KEY, ARTIFACT_PROPERTY_OVERRIDE_VALUE).build();
  private Map<Object, Object> springPropertiesOverriddenMap =
      ImmutableMap.builder().put(OVERRIDE_PROPERTY_KEY, SPRING_PROPERTY_OVERRIDE_VALUE).build();
  private DefaultArtifactProperties properties;

  @Test
  public void propertiesScopesOverride() throws Exception {
    properties =
        new DefaultArtifactProperties(configPropertiesOverriddenPropertyMap, emptyMap(), emptyMap());
    assertThat(properties.getProperty(OVERRIDE_PROPERTY_KEY), is(CONFIG_PROPERTY_OVERRIDE_VALUE));

    properties = new DefaultArtifactProperties(configPropertiesOverriddenPropertyMap, springPropertiesOverriddenMap, emptyMap());
    assertThat(properties.getProperty(OVERRIDE_PROPERTY_KEY), is(SPRING_PROPERTY_OVERRIDE_VALUE));

    properties = new DefaultArtifactProperties(configPropertiesOverriddenPropertyMap, springPropertiesOverriddenMap,
                                               artifactPropertiesOverriddenMap);
    assertThat(properties.getProperty(OVERRIDE_PROPERTY_KEY), is(ARTIFACT_PROPERTY_OVERRIDE_VALUE));


    testWithSystemProperty(OVERRIDE_PROPERTY_KEY, SYSTEM_PROPERTY_OVERRIDE_VALUE, () -> {
      properties = new DefaultArtifactProperties(configPropertiesOverriddenPropertyMap, springPropertiesOverriddenMap,
                                                 artifactPropertiesOverriddenMap);
      assertThat(properties.getProperty(OVERRIDE_PROPERTY_KEY), is(SYSTEM_PROPERTY_OVERRIDE_VALUE));

    });
  }

  @Test(expected = UnsupportedOperationException.class)
  public void propertiesMapReturnsInmmutableMap() {
    new DefaultArtifactProperties(emptyMap(), emptyMap(), emptyMap()).toImmutableMap().put("key", "value");
  }

  @Test(expected = IllegalArgumentException.class)
  public void configMapCannotBeNull() {
    new DefaultArtifactProperties(null, emptyMap(), emptyMap());
  }

  @Test(expected = IllegalArgumentException.class)
  public void springPropertiesMapCannotBeNull() {
    new DefaultArtifactProperties(emptyMap(), null, emptyMap());
  }

  @Test(expected = IllegalArgumentException.class)
  public void artifactConfigMapCannotBeNull() {
    new DefaultArtifactProperties(emptyMap(), emptyMap(), null);
  }

}
