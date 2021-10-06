/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.meta.model.stereotype.StereotypeModelBuilder.newStereotype;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.PROCESSOR;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.SOURCE;
import static org.mule.runtime.internal.dsl.DslConstants.CONFIG_ATTRIBUTE_NAME;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.declaration.fluent.ComponentDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ComponentDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ConnectionProviderDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.HasModelProperties;
import org.mule.runtime.api.meta.model.declaration.fluent.HasStereotypeDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.NestedComponentDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.StereotypedDeclaration;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.extension.api.declaration.type.DefaultExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.internal.loader.parser.AllowedStereotypesModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.StereotypeModelFactory;
import org.mule.runtime.module.extension.internal.loader.parser.StereotypeModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.java.stereotypes.CustomStereotypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.java.stereotypes.DefaultStereotypeModelFactory;
import org.mule.sdk.api.stereotype.MuleStereotypes;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;

public class StereotypeModelLoaderDelegate {

  private String namespace;

  private final Map<ComponentDeclaration, List<StereotypeModel>> componentsConfigStereotypes = new HashMap<>();
  private final StereotypeModel sourceParent;
  private final StereotypeModel processorParent;
  private final ClassTypeLoader typeLoader;
  private final DslResolvingContext dslResolvingContext;
  private final StereotypeModelFactory stereotypeModelFactory;

  public StereotypeModelLoaderDelegate(ExtensionLoadingContext extensionLoadingContext) {
    stereotypeModelFactory = new DefaultStereotypeModelFactory(extensionLoadingContext);
    dslResolvingContext = extensionLoadingContext.getDslResolvingContext();
    this.typeLoader =
        new DefaultExtensionsTypeLoaderFactory().createTypeLoader(extensionLoadingContext.getExtensionClassLoader());
    ExtensionDeclaration declaration = extensionLoadingContext.getExtensionDeclarer().getDeclaration();
    this.processorParent = newStereotype(PROCESSOR.getType(), namespace).withParent(PROCESSOR).build();
    this.sourceParent = newStereotype(SOURCE.getType(), namespace).withParent(SOURCE).build();

    resolveDeclaredTypesStereotypes(declaration, namespace);
  }

  public StereotypeModel getProcessorParentStereotype() {
    return stereotypeModelFactory.getProcessorParentStereotype();
  }

  public StereotypeModel createStereotype(String name, StereotypeModel parent) {
    return newStereotype(name, namespace).withParent(parent).build();
  }

  public void addStereotype(StereotypeModelParser parser,
                            ConfigurationDeclarer declarer,
                            Optional<Supplier<StereotypeModel>> fallback) {
    doAddStereotypes(parser, declarer, fallback);
    populateComponentConfigsMap(declarer.getDeclaration());
  }

  public void addStereotype(StereotypeModelParser parser,
                            ConnectionProviderDeclarer declarer,
                            Optional<Supplier<StereotypeModel>> fallback) {
    doAddStereotypes(parser, declarer, fallback);
  }

  public void addStereotype(StereotypeModelParser parser,
                            ComponentDeclarer declarer,
                            Optional<Supplier<StereotypeModel>> fallback) {
    doAddStereotypes(parser, declarer, fallback);
    addConfigRefStereoTypesIfNeeded((ComponentDeclaration<?>) declarer.getDeclaration());
  }

  public void addAllowedStereotypes(AllowedStereotypesModelParser parser,
                                    NestedComponentDeclarer declarer) {
    List<StereotypeModel> allowedStereotypes = parser.getAllowedStereotypes(stereotypeModelFactory);
    if (allowedStereotypes.isEmpty()) {
      declarer.withAllowedStereotypes(MuleStereotypes.PROCESSOR);
    } else {
      allowedStereotypes.forEach(declarer::withAllowedStereotypes);
    }
  }

  private void addConfigRefStereoTypesIfNeeded(ComponentDeclaration<?> declaration) {
    List<StereotypeModel> configStereotypes = componentsConfigStereotypes.get(declaration);
    if (configStereotypes != null && !configStereotypes.isEmpty()) {
      declaration.getAllParameters().stream()
          .filter(p -> CONFIG_ATTRIBUTE_NAME.equals(p.getName()))
          .findAny()
          .ifPresent(configRef -> configRef.setAllowedStereotypeModels(configStereotypes));
    }
  }

  private <T extends HasStereotypeDeclarer & HasModelProperties> StereotypeModel doAddStereotypes(
      StereotypeModelParser parser,
      T declarer,
      Optional<Supplier<StereotypeModel>> fallback) {


    StereotypeModel stereotypeModel = parser.getStereotype(stereotypeModelFactory).orElse(null);
    if (stereotypeModel != null) {
      declarer.withModelProperty(CustomStereotypeModelProperty.INSTANCE);
    } else {
      stereotypeModel = fallback.map(Supplier::get).orElse(null);
    }

    if (stereotypeModel != null) {
      declarer.withStereotype(stereotypeModel);
    }

    return stereotypeModel;
  }

  private void populateComponentConfigsMap(ConfigurationDeclaration config) {
    StereotypeModel configStereotype = config.getStereotype();
    if (configStereotype != null) {
      config.getConstructs().forEach(construct -> addComponentConfigStereotype(construct, configStereotype));
      config.getMessageSources().forEach(source -> addComponentConfigStereotype(source, configStereotype));
      config.getOperations().forEach(operation -> addComponentConfigStereotype(operation, configStereotype));
    }
  }

  private void addComponentConfigStereotype(ComponentDeclaration declaration, StereotypeModel configStereotype) {
    componentsConfigStereotypes.computeIfAbsent(declaration, key -> new LinkedList<>()).add(configStereotype);
  }

  private String getStereotypePrefix(ExtensionDeclarer extensionDeclarer) {
    return extensionDeclarer.getDeclaration().getXmlDslModel().getPrefix().toUpperCase();
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace.toUpperCase();
  }
}
