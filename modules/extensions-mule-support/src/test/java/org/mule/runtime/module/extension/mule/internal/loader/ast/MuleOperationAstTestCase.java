/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.ast;

import static org.mule.runtime.ast.api.xml.AstXmlParser.builder;

import static java.lang.Thread.currentThread;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.xml.AstXmlParser;
import org.mule.runtime.core.api.extension.RuntimeExtensionModelProvider;
import org.mule.runtime.core.api.registry.SpiServiceRegistry;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class MuleOperationAstTestCase extends AbstractMuleTestCase {

  private ClassLoader classLoader;
  private AstXmlParser parser;

  private final Map<String, String> properties = new HashMap<>();

  private static List<ExtensionModel> runtimeExtensionModels;

  @BeforeClass
  public static void beforeClass() throws Exception {
    runtimeExtensionModels = new ArrayList<>();
    Collection<RuntimeExtensionModelProvider> runtimeExtensionModelProviders = new SpiServiceRegistry()
        .lookupProviders(RuntimeExtensionModelProvider.class, currentThread().getContextClassLoader());
    for (RuntimeExtensionModelProvider runtimeExtensionModelProvider : runtimeExtensionModelProviders) {
      runtimeExtensionModels.add(runtimeExtensionModelProvider.createExtensionModel());
    }
  }

  @Before
  public void before() {
    properties.clear();
    classLoader = MuleOperationAstTestCase.class.getClassLoader();

    parser = builder()
        .withSchemaValidationsDisabled()
        .withExtensionModels(runtimeExtensionModels)
        .withPropertyResolver(propertyKey -> properties.getOrDefault(propertyKey, propertyKey))
        .build();
  }

  @Test
  public void operationDeprecation() {
    ArtifactAst appAst = parser.parse(classLoader.getResource("mule-deprecations-config.xml"));
    ComponentAst deprecatedOperationAst = getTopLevelComponent(appAst, "deprecatedOperation");
    ComponentAst deprecationAst = getChild(deprecatedOperationAst, "deprecated");
    ComponentModel deprecationComponentModel = deprecationAst.getModel(ComponentModel.class).get();
    assertThat(deprecationComponentModel.getDescription(), is("Defines an operation's deprecation."));
  }

  @Test
  public void parameterDeprecation() {
    ArtifactAst appAst = parser.parse(classLoader.getResource("mule-deprecations-config.xml"));
    ComponentAst operationWithDeprecatedParameter = getTopLevelComponent(appAst, "operationWithDeprecatedParameter");
    ComponentAst parametersAst = getChild(operationWithDeprecatedParameter, "parameters");
    ComponentAst deprecatedParameterAst = getChild(parametersAst, "parameter");
    ComponentAst deprecationAst = getChild(deprecatedParameterAst, "deprecated");
    ComponentModel deprecationComponentModel = deprecationAst.getModel(ComponentModel.class).get();
    assertThat(deprecationComponentModel.getDescription(), is("Defines a parameter's deprecation."));
  }

  private ComponentAst getTopLevelComponent(ArtifactAst ast, String componentName) {
    return ast.topLevelComponentsStream().filter(componentAst -> componentAst.getComponentId().get().equals(componentName))
        .findFirst().get();
  }

  private ComponentAst getChild(ComponentAst ast, String childName) {
    return ast.directChildrenStreamByIdentifier(null, childName).findFirst().get();
  }
}
