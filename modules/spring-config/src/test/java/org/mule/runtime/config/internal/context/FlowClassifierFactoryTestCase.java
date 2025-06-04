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
import static org.mule.runtime.core.internal.management.stats.FlowClassifier.FlowType.APIKIT;
import static org.mule.runtime.core.internal.management.stats.FlowClassifier.FlowType.GENERIC;
import static org.mule.runtime.core.internal.management.stats.FlowClassifier.FlowType.SOAPKIT;

import static java.util.Collections.singletonList;
import static java.util.Collections.emptyList;
import static java.util.Arrays.asList;
import static java.util.Optional.of;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.core.internal.management.stats.FlowClassifier;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class FlowClassifierFactoryTestCase extends AbstractMuleTestCase {

  private static final String APIKIT_EXTENSION_NAME = "APIKit";
  private static final String SOAPKIT_EXTENSION_NAME = "APIKit for SOAP";

  private ArtifactAst artifactAst;
  private ArtifactAst parentArtifactAst;
  private ComponentAst apiKitConfig;
  private ComponentAst soapKitConfig;
  private ComponentAst parentApiKitConfig;
  private ComponentAst parentSoapKitConfig;

  @Before
  public void setUp() {
    artifactAst = mock(ArtifactAst.class);
    parentArtifactAst = mock(ArtifactAst.class);
    apiKitConfig = mock(ComponentAst.class);
    soapKitConfig = mock(ComponentAst.class);
    parentApiKitConfig = mock(ComponentAst.class);
    parentSoapKitConfig = mock(ComponentAst.class);

    setupConfigMock(apiKitConfig, "api-config", APIKIT_EXTENSION_NAME);
    setupConfigMock(soapKitConfig, "soapkit-config", SOAPKIT_EXTENSION_NAME);
    setupConfigMock(parentApiKitConfig, "parent-api-config", APIKIT_EXTENSION_NAME);
    setupConfigMock(parentSoapKitConfig, "parent-soapkit-config", SOAPKIT_EXTENSION_NAME);

    // Setup APIKit config with flow mappings
    ComponentAst flowMapping = mock(ComponentAst.class);
    ComponentParameterAst flowRefParameter = mock(ComponentParameterAst.class);
    when(flowRefParameter.getValue()).thenReturn(right("mapped-flow"));
    when(flowMapping.getParameter("FlowMapping", "flow-ref")).thenReturn(flowRefParameter);
    ComponentParameterAst flowMappingsParameter = mock(ComponentParameterAst.class);
    when(flowMappingsParameter.getValue()).thenReturn(right(singletonList(flowMapping)));
    when(apiKitConfig.getParameter(DEFAULT_GROUP_NAME, "flowMappings")).thenReturn(flowMappingsParameter);

    // Setup parent APIKit config with flow mappings
    ComponentAst parentFlowMapping = mock(ComponentAst.class);
    ComponentParameterAst parentFlowRefParameter = mock(ComponentParameterAst.class);
    when(parentFlowRefParameter.getValue()).thenReturn(right("parent-mapped-flow"));
    when(parentFlowMapping.getParameter("FlowMapping", "flow-ref")).thenReturn(parentFlowRefParameter);
    ComponentParameterAst parentFlowMappingsParameter = mock(ComponentParameterAst.class);
    when(parentFlowMappingsParameter.getValue()).thenReturn(right(singletonList(parentFlowMapping)));
    when(parentApiKitConfig.getParameter(DEFAULT_GROUP_NAME, "flowMappings")).thenReturn(parentFlowMappingsParameter);
  }

  private FlowClassifier createClassifier(List<ComponentAst> components, List<ComponentAst> parentComponents) {
    when(artifactAst.topLevelComponentsStream()).thenAnswer(invocation -> components.stream());
    when(artifactAst.getParent()).thenReturn(of(parentArtifactAst));
    when(parentArtifactAst.topLevelComponentsStream()).thenAnswer(invocation -> parentComponents.stream());
    return new FlowClassifierFactory(artifactAst).create();
  }

  @Test
  public void testGenericFlow() {
    FlowClassifier classifier = createClassifier(emptyList(), emptyList());
    assertThat("generic-flow", classifier.getFlowType("generic-flow"), is(GENERIC));
  }

  @Test
  public void testApiKitFlow() {
    FlowClassifier classifier = createClassifier(singletonList(apiKitConfig), emptyList());
    assertThat("mapped-flow", classifier.getFlowType("mapped-flow"), is(APIKIT));
    assertThat("flow:api-config", classifier.getFlowType("flow:api-config"), is(APIKIT));
  }

  @Test
  public void testSoapKitFlow() {
    FlowClassifier classifier = createClassifier(singletonList(soapKitConfig), emptyList());
    assertThat("flow:\\soapkit-config", classifier.getFlowType("flow:\\soapkit-config"), is(SOAPKIT));
  }

  @Test
  public void testMultipleConfigs() {
    FlowClassifier classifier = createClassifier(asList(apiKitConfig, soapKitConfig), emptyList());
    assertThat("mapped-flow", classifier.getFlowType("mapped-flow"), is(APIKIT));
    assertThat("flow:api-config", classifier.getFlowType("flow:api-config"), is(APIKIT));
    assertThat("flow:\\soapkit-config", classifier.getFlowType("flow:\\soapkit-config"), is(SOAPKIT));
    assertThat("generic-flow", classifier.getFlowType("generic-flow"), is(GENERIC));
  }

  @Test
  public void testApiKitFlowWithNonMatchingConfig() {
    FlowClassifier classifier = createClassifier(singletonList(apiKitConfig), emptyList());
    assertThat("flow:non-matching-config", classifier.getFlowType("flow:non-matching-config"), is(GENERIC));
  }

  @Test
  public void testSoapKitFlowWithNonMatchingConfig() {
    FlowClassifier classifier = createClassifier(singletonList(soapKitConfig), emptyList());
    assertThat("flow:\\non-matching-config", classifier.getFlowType("flow:\\non-matching-config"), is(GENERIC));
  }

  @Test
  public void testMultipleConfigsWithNonMatchingFlows() {
    FlowClassifier classifier = createClassifier(asList(apiKitConfig, soapKitConfig), emptyList());
    assertThat("flow:non-matching-api-config", classifier.getFlowType("flow:non-matching-api-config"), is(GENERIC));
    assertThat("flow:\\non-matching-soap-config", classifier.getFlowType("flow:\\non-matching-soap-config"), is(GENERIC));
  }

  @Test
  public void testApiKitFlowFromParent() {
    FlowClassifier classifier = createClassifier(emptyList(), singletonList(parentApiKitConfig));
    assertThat("parent-mapped-flow", classifier.getFlowType("parent-mapped-flow"), is(APIKIT));
    assertThat("flow:parent-api-config", classifier.getFlowType("flow:parent-api-config"), is(APIKIT));
  }

  @Test
  public void testSoapKitFlowFromParent() {
    FlowClassifier classifier = createClassifier(emptyList(), singletonList(parentSoapKitConfig));
    assertThat("flow:\\parent-soapkit-config", classifier.getFlowType("flow:\\parent-soapkit-config"), is(SOAPKIT));
  }

  @Test
  public void testMultipleConfigsFromParent() {
    FlowClassifier classifier = createClassifier(emptyList(), asList(parentApiKitConfig, parentSoapKitConfig));
    assertThat("parent-mapped-flow", classifier.getFlowType("parent-mapped-flow"), is(APIKIT));
    assertThat("flow:parent-api-config", classifier.getFlowType("flow:parent-api-config"), is(APIKIT));
    assertThat("flow:\\parent-soapkit-config", classifier.getFlowType("flow:\\parent-soapkit-config"), is(SOAPKIT));
    assertThat("generic-flow", classifier.getFlowType("generic-flow"), is(GENERIC));
  }

  @Test
  public void testConfigsFromBothMainAndParent() {
    FlowClassifier classifier = createClassifier(asList(apiKitConfig, soapKitConfig),
                                                 asList(parentApiKitConfig, parentSoapKitConfig));
    assertThat("mapped-flow", classifier.getFlowType("mapped-flow"), is(APIKIT));
    assertThat("flow:api-config", classifier.getFlowType("flow:api-config"), is(APIKIT));
    assertThat("flow:\\soapkit-config", classifier.getFlowType("flow:\\soapkit-config"), is(SOAPKIT));
    assertThat("parent-mapped-flow", classifier.getFlowType("parent-mapped-flow"), is(APIKIT));
    assertThat("flow:parent-api-config", classifier.getFlowType("flow:parent-api-config"), is(APIKIT));
    assertThat("flow:\\parent-soapkit-config", classifier.getFlowType("flow:\\parent-soapkit-config"), is(SOAPKIT));
    assertThat("generic-flow", classifier.getFlowType("generic-flow"), is(GENERIC));
  }

  private void setupConfigMock(ComponentAst config, String configId, String extensionName) {
    when(config.getComponentType()).thenReturn(CONFIG);
    when(config.getComponentId()).thenReturn(of(configId));
    ExtensionModel extensionModel = mock(ExtensionModel.class);
    when(extensionModel.getName()).thenReturn(extensionName);
    when(config.getExtensionModel()).thenReturn(extensionModel);
    ConfigurationModel configModel = mock(ConfigurationModel.class);
    when(configModel.getName()).thenReturn("config");
    when(config.getModel(ConfigurationModel.class)).thenReturn(of(configModel));
  }
}
