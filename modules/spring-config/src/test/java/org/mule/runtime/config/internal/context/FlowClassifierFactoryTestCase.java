/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.context;

import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.CONFIG;
import static org.mule.runtime.api.functional.Either.right;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.core.api.management.stats.FlowClassifier.FlowType.APIKIT;
import static org.mule.runtime.core.api.management.stats.FlowClassifier.FlowType.GENERIC;
import static org.mule.runtime.core.api.management.stats.FlowClassifier.FlowType.SOAPKIT;

import static java.util.Collections.singletonList;
import static java.util.Collections.emptyList;
import static java.util.Arrays.asList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.core.api.management.stats.FlowClassifier;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

public class FlowClassifierFactoryTestCase extends AbstractMuleTestCase {

  private ArtifactAst artifactAst;
  private ComponentAst apiKitConfig;
  private ComponentAst soapKitConfig;

  @Before
  public void setUp() {
    artifactAst = mock(ArtifactAst.class);
    apiKitConfig = mock(ComponentAst.class);
    soapKitConfig = mock(ComponentAst.class);

    setupConfigMock(apiKitConfig, "api-config", "APIKit");
    setupConfigMock(soapKitConfig, "soapkit-config", "APIKit for SOAP");

    // Setup APIKit config with flow mappings
    ComponentAst flowMapping = mock(ComponentAst.class);
    ComponentParameterAst flowRefParameter = mock(ComponentParameterAst.class);
    when(flowRefParameter.getValue()).thenReturn(right("mapped-flow"));
    when(flowMapping.getParameter("FlowMapping", "flow-ref")).thenReturn(flowRefParameter);
    ComponentParameterAst flowMappingsParameter = mock(ComponentParameterAst.class);
    when(flowMappingsParameter.getValue()).thenReturn(right(singletonList(flowMapping)));
    when(apiKitConfig.getParameter(DEFAULT_GROUP_NAME, "flowMappings")).thenReturn(flowMappingsParameter);
  }

  private FlowClassifier createClassifier(java.util.List<ComponentAst> components) {
    when(artifactAst.topLevelComponentsStream()).thenAnswer(invocation -> components.stream());
    return new FlowClassifierFactory(artifactAst).create();
  }

  @Test
  public void testGenericFlow() {
    FlowClassifier classifier = createClassifier(emptyList());
    assertThat("generic-flow", classifier.getFlowType("generic-flow"), is(GENERIC));
  }

  @Test
  public void testApiKitFlow() {
    FlowClassifier classifier = createClassifier(singletonList(apiKitConfig));
    assertThat("mapped-flow", classifier.getFlowType("mapped-flow"), is(APIKIT));
    assertThat("flow:api-config", classifier.getFlowType("flow:api-config"), is(APIKIT));
  }

  @Test
  public void testSoapKitFlow() {
    FlowClassifier classifier = createClassifier(singletonList(soapKitConfig));
    assertThat("flow:\\soapkit-config", classifier.getFlowType("flow:\\soapkit-config"), is(SOAPKIT));
  }

  @Test
  public void testMultipleConfigs() {
    FlowClassifier classifier = createClassifier(asList(apiKitConfig, soapKitConfig));
    assertThat("mapped-flow", classifier.getFlowType("mapped-flow"), is(APIKIT));
    assertThat("flow:api-config", classifier.getFlowType("flow:api-config"), is(APIKIT));
    assertThat("flow:\\soapkit-config", classifier.getFlowType("flow:\\soapkit-config"), is(SOAPKIT));
    assertThat("generic-flow", classifier.getFlowType("generic-flow"), is(GENERIC));
  }

  @Test
  public void testApiKitFlowWithNonMatchingConfig() {
    FlowClassifier classifier = createClassifier(singletonList(apiKitConfig));
    assertThat("flow:non-matching-config", classifier.getFlowType("flow:non-matching-config"), is(GENERIC));
  }

  @Test
  public void testSoapKitFlowWithNonMatchingConfig() {
    FlowClassifier classifier = createClassifier(singletonList(soapKitConfig));
    assertThat("flow:\\non-matching-config", classifier.getFlowType("flow:\\non-matching-config"), is(GENERIC));
  }

  @Test
  public void testMultipleConfigsWithNonMatchingFlows() {
    FlowClassifier classifier = createClassifier(asList(apiKitConfig, soapKitConfig));
    assertThat("flow:non-matching-api-config", classifier.getFlowType("flow:non-matching-api-config"), is(GENERIC));
    assertThat("flow:\\non-matching-soap-config", classifier.getFlowType("flow:\\non-matching-soap-config"), is(GENERIC));
  }

  private void setupConfigMock(ComponentAst config, String configId, String extensionName) {
    when(config.getComponentType()).thenReturn(CONFIG);
    when(config.getComponentId()).thenReturn(Optional.of(configId));
    ExtensionModel extensionModel = mock(ExtensionModel.class);
    when(extensionModel.getName()).thenReturn(extensionName);
    when(config.getExtensionModel()).thenReturn(extensionModel);
    ConfigurationModel configModel = mock(ConfigurationModel.class);
    when(configModel.getName()).thenReturn("config");
    when(config.getModel(ConfigurationModel.class)).thenReturn(Optional.of(configModel));
  }
}
