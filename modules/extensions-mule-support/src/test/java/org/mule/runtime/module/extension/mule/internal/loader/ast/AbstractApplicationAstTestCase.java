/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.ast;

import static org.mule.runtime.ast.api.xml.AstXmlParser.builder;

import static java.lang.Thread.currentThread;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.xml.AstXmlParser;
import org.mule.runtime.ast.api.xml.AstXmlParser.Builder;
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

/**
 * Extend this class to make assertions on the AST generated from an application's XML config file.
 */
public abstract class AbstractApplicationAstTestCase extends AbstractMuleTestCase {

  private ClassLoader classLoader;
  private AstXmlParser parser;

  private final Map<String, String> properties = new HashMap<>();

  private static List<ExtensionModel> runtimeExtensionModels;

  protected abstract String getConfigFile();

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
    classLoader = this.getClass().getClassLoader();

    Builder astParserBuilder = builder()
        .withSchemaValidationsDisabled()
        .withExtensionModels(runtimeExtensionModels)
        .withPropertyResolver(propertyKey -> properties.getOrDefault(propertyKey, propertyKey));
    customizeAstParserBuilder(astParserBuilder);
    parser = astParserBuilder.build();
  }

  protected void customizeAstParserBuilder(Builder astParserBuilder) {
    // nothing to do here, override this method if you want to customize the builder (for example, to add a custom extension
    // model)
  }

  protected ArtifactAst getApplicationAst() {
    return parser.parse(classLoader.getResource(getConfigFile()));
  }

  protected ComponentAst getTopLevelComponent(ArtifactAst ast, String componentName) {
    return ast.topLevelComponentsStream().filter(componentAst -> componentAst.getComponentId().get().equals(componentName))
        .findFirst().get();
  }

  protected ComponentAst getChild(ComponentAst ast, String childName) {
    return ast.directChildrenStreamByIdentifier(null, childName).findFirst().get();
  }
}
