/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.api;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.FLOW_ELEMENT;
import static org.mule.runtime.config.spring.dsl.processor.xml.CoreXmlNamespaceInfoProvider.CORE_NAMESPACE_NAME;
import org.mule.runtime.config.spring.SpringXmlConfigurationBuilder;
import org.mule.runtime.config.spring.dsl.api.config.ArtifactConfiguration;
import org.mule.runtime.config.spring.dsl.api.config.ComponentConfiguration;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.config.bootstrap.ArtifactType;
import org.mule.runtime.core.context.DefaultMuleContextFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

import org.junit.Test;

public class CoreProcessorParsingTestCase extends AbstractMuleTestCase {

  public static final String TEST_FLOW_NAME = "testFlow";
  public static final String PRIVATE_FLOW_NAME = "privateFlow";

  @Test
  public void loggerConfiguration() throws Exception {
    ComponentConfiguration flowConfiguration = new ComponentConfiguration.Builder()
        .setNamespace(CORE_NAMESPACE_NAME)
        .setIdentifier(FLOW_ELEMENT)
        .addParameter("name", TEST_FLOW_NAME)
        .addNestedConfiguration(new ComponentConfiguration.Builder()
            .setNamespace(CORE_NAMESPACE_NAME)
            .setIdentifier("logger")
            .addParameter("message", "${logger.message}")
            .addParameter("level", "${logger.level}")
            .addParameter("category", "${logger.category}")
            .build())
        .build();
    ImmutableMap<String, String> applicationProperties = ImmutableMap.<String, String>builder()
        .put("logger.message", "message")
        .put("logger.level", "level")
        .put("logger.category", "category")
        .build();

    ArtifactConfiguration artifactConfiguration = new ArtifactConfiguration(asList(flowConfiguration));
    validateConfiguration(applicationProperties, artifactConfiguration);
  }

  @Test
  public void flowRefConfiguration() throws Exception {
    ComponentConfiguration flowConfiguration = new ComponentConfiguration.Builder()
        .setNamespace(CORE_NAMESPACE_NAME)
        .setIdentifier(FLOW_ELEMENT)
        .addParameter("name", TEST_FLOW_NAME)
        .addNestedConfiguration(new ComponentConfiguration.Builder()
            .setNamespace("mule")
            .setIdentifier("flow-ref")
            .setNamespace("mule")
            .addParameter("name", PRIVATE_FLOW_NAME)
            .build())
        .build();

    ComponentConfiguration privateFlowConfiguration = new ComponentConfiguration.Builder()
        .setNamespace(CORE_NAMESPACE_NAME)
        .setIdentifier(FLOW_ELEMENT)
        .addParameter("name", PRIVATE_FLOW_NAME)
        .addNestedConfiguration(new ComponentConfiguration.Builder()
            .setNamespace(CORE_NAMESPACE_NAME)
            .setIdentifier("set-payload")
            .addParameter("value", "hello")
            .build())
        .build();

    ArtifactConfiguration artifactConfiguration = new ArtifactConfiguration(asList(flowConfiguration, privateFlowConfiguration));
    validateConfiguration(emptyMap(), artifactConfiguration);
  }

  private void validateConfiguration(Map<String, String> applicationProperties,
                                     ArtifactConfiguration artifactConfiguration)
      throws MuleException {
    MuleContext muleContext = null;
    try {
      SpringXmlConfigurationBuilder springXmlConfigurationBuilder =
          new SpringXmlConfigurationBuilder(new String[0], artifactConfiguration, applicationProperties, ArtifactType.APP, false);
      muleContext = new DefaultMuleContextFactory().createMuleContext(springXmlConfigurationBuilder);
      muleContext.start();
    } finally {
      if (muleContext != null) {
        try {
          muleContext.dispose();
        } catch (Exception e) {
          // so we don't hide the real problem
        }
      }
    }
  }

}
