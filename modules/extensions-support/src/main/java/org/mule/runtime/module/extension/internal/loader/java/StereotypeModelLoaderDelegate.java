/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import static org.mule.runtime.api.meta.model.stereotype.StereotypeModelBuilder.newStereotype;
import static org.mule.runtime.api.util.FunctionalUtils.ifPresent;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.CONFIG;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.PROCESSOR;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.SOURCE;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.VALIDATOR;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.VALIDATOR_DEFINITION;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.declaration.fluent.ComponentDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.HasModelProperties;
import org.mule.runtime.api.meta.model.declaration.fluent.HasStereotypeDeclarer;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.extension.api.declaration.type.DefaultExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.stereotype.MuleStereotypes;
import org.mule.runtime.extension.api.stereotype.StereotypeDefinition;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionTypeDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.StereotypeModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.java.stereotypes.ClassStereotypeResolver;
import org.mule.runtime.module.extension.internal.loader.parser.java.stereotypes.CustomStereotypeModelProperty;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;

public class StereotypeModelLoaderDelegate {

  private final Map<StereotypeDefinition, StereotypeModel> stereotypes = new HashMap<>();
  private final String namespace;
  private final Multimap<ComponentDeclaration, ConfigurationDeclaration> componentConfigs = LinkedListMultimap.create();
  private final StereotypeModel sourceParent;
  private final StereotypeModel processorParent;
  private final ClassTypeLoader typeLoader;
  private final DslResolvingContext dslResolvingContext;
  private final LazyValue<StereotypeModel> validatorStereotype;

  public StereotypeModelLoaderDelegate(ExtensionLoadingContext extensionLoadingContext, String namespace) {
    dslResolvingContext = extensionLoadingContext.getDslResolvingContext();
    ExtensionDeclarer extensionDeclarer = extensionLoadingContext.getExtensionDeclarer();
    this.typeLoader =
        new DefaultExtensionsTypeLoaderFactory().createTypeLoader(extensionLoadingContext.getExtensionClassLoader());
    ExtensionDeclaration declaration = extensionLoadingContext.getExtensionDeclarer().getDeclaration();
    this.namespace = getStereotypePrefix(extensionDeclarer);
    this.processorParent = newStereotype(PROCESSOR.getType(), namespace).withParent(PROCESSOR).build();
    this.sourceParent = newStereotype(SOURCE.getType(), namespace).withParent(SOURCE).build();
    validatorStereotype = new LazyValue<>(() -> newStereotype(VALIDATOR_DEFINITION.getName(), namespace)
        .withParent(VALIDATOR)
        .build());


    resolveDeclaredTypesStereotypes(declaration, namespace);
  }

  private <T extends HasStereotypeDeclarer & HasModelProperties> void resolveStereotype(StereotypeModelParser parser,
                                                                                      T declarer,
                                                                                      Optional<Supplier<StereotypeModel>> fallback) {

    StereotypeModelParser.ParsedStereotype stereotype = parser.getParsedStereotype();
    if (stereotype.isValidator() || stereotype.getStereotypeModel().isPresent()) {
      declarer.withModelProperty(new CustomStereotypeModelProperty());
    }

    StereotypeModel model = stereotype.getStereotypeModel()
        .orElseGet(() -> {
          if (stereotype.isValidator()) {
            return getValidatorStereotype();
          } else {
            return fallback.map(Supplier::get).orElse(null);
          }
        });

    if (model != null) {
      declarer.withStereotype(model);
    }
    final StereotypeModel defaultStereotype = createStereotype(config.getName(), CONFIG);
    ifPresent(config.getModelProperty(ExtensionTypeDescriptorModelProperty.class)
            .map(ExtensionTypeDescriptorModelProperty::getType),
        type -> resolveStereotype(type, config, defaultStereotype),
        () -> config.withStereotype(defaultStereotype));
    componentConfigs = populateComponentConfigsMap(config);

  }

  private void resolveStereotype(Type type) {
    new ClassStereotypeResolver(type, declaration, namespace, stereotypes).resolveStereotype();
  }

  private String getStereotypePrefix(ExtensionDeclarer extensionDeclarer) {
    return extensionDeclarer.getDeclaration().getXmlDslModel().getPrefix().toUpperCase();
  }

  public StereotypeModel getValidatorStereotype() {
    return validatorStereotype.get();
  }
}
