/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toMap;
import static org.mule.runtime.api.meta.model.stereotype.StereotypeModelBuilder.newStereotype;
import static org.mule.runtime.api.util.FunctionalUtils.computeIfAbsent;
import static org.mule.runtime.core.api.util.StringUtils.isBlank;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypeDefinition.NAMESPACE;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.PROCESSOR;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.SOURCE;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.VALIDATOR;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.VALIDATOR_DEFINITION;
import static org.mule.runtime.module.extension.internal.loader.java.MuleExtensionAnnotationParser.mapReduceExtensionAnnotation;
import static org.mule.runtime.module.extension.internal.loader.utils.JavaModelLoaderUtils.isProcessorChain;
import static org.mule.runtime.module.extension.internal.loader.utils.JavaModelLoaderUtils.isRoute;

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
import org.mule.runtime.api.meta.model.declaration.fluent.NamedDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.NestableElementDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.NestedChainDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.NestedRouteDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.WithAllowedStereotypesDeclaration;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModelBuilder;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.extension.api.annotation.param.stereotype.AllowedStereotypes;
import org.mule.runtime.extension.api.annotation.param.stereotype.Stereotype;
import org.mule.runtime.extension.api.annotation.param.stereotype.Validator;
import org.mule.runtime.extension.api.declaration.type.DefaultExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.api.loader.java.type.MethodElement;
import org.mule.runtime.module.extension.api.loader.java.type.WithAnnotations;
import org.mule.runtime.module.extension.internal.loader.parser.StereotypeModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.StereotypeModelParser.ParsedStereotype;
import org.mule.runtime.module.extension.internal.loader.parser.java.stereotypes.CustomStereotypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.java.stereotypes.SdkStereotypeDefinitionAdapter;
import org.mule.sdk.api.stereotype.StereotypeDefinition;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;

public class StereotypeModelLoaderDelegate {

  private String namespace;

  private final Map<StereotypeDefinition, StereotypeModel> stereotypesCache = new HashMap<>();
  private final Multimap<ComponentDeclaration, ConfigurationDeclaration> componentConfigs = LinkedListMultimap.create();
  private final StereotypeModel sourceParent;
  private final StereotypeModel processorParent;
  private final ClassTypeLoader typeLoader;
  private final DslResolvingContext dslResolvingContext;
  private final LazyValue<StereotypeModel> validatorStereotype;

  public StereotypeModelLoaderDelegate(ExtensionLoadingContext extensionLoadingContext) {
    dslResolvingContext = extensionLoadingContext.getDslResolvingContext();
    this.typeLoader =
        new DefaultExtensionsTypeLoaderFactory().createTypeLoader(extensionLoadingContext.getExtensionClassLoader());
    ExtensionDeclaration declaration = extensionLoadingContext.getExtensionDeclarer().getDeclaration();
    this.processorParent = newStereotype(PROCESSOR.getType(), namespace).withParent(PROCESSOR).build();
    this.sourceParent = newStereotype(SOURCE.getType(), namespace).withParent(SOURCE).build();
    validatorStereotype = new LazyValue<>(() -> newStereotype(VALIDATOR_DEFINITION.getName(), namespace)
        .withParent(VALIDATOR)
        .build());


    resolveDeclaredTypesStereotypes(declaration, namespace);
  }

  public ParsedStereotype resolveStereotype(WithAnnotations annotatedElement,
                                            String elementType,
                                            String elementName) {
    StereotypeDefinition stereotypeDefinition = mapReduceExtensionAnnotation(
        annotatedElement,
        elementType,
        elementName,
        Stereotype.class,
        org.mule.sdk.api.annotation.param.stereotype.Stereotype.class,
        value -> value.getClassValue(Stereotype::value),
        value -> value.getClassValue(org.mule.sdk.api.annotation.param.stereotype.Stereotype::value)
    ).flatMap(type -> type.getDeclaringClass())
        .map(SdkStereotypeDefinitionAdapter::from)
        .orElse(null);

    boolean isValidator = annotatedElement.isAnnotatedWith(Validator.class)
        || annotatedElement.isAnnotatedWith(org.mule.sdk.api.annotation.param.stereotype.Validator.class);

    if (stereotypeDefinition != null && isValidator) {
      throw new IllegalModelDefinitionException(format("%s '%s' is annotated with both @%s and @%s. Only one can "
              + "be provided at the same time for the same component",
          elementType, elementName, Stereotype.class.getSimpleName(), Validator.class.getSimpleName()));
    }

    if (isValidator) {
      return new DefaultParsedStereotype(empty(), true);
    }
    return new DefaultParsedStereotype(of(toStereotypeModel(stereotypeDefinition, namespace)), false);
  }

  private StereotypeModel toStereotypeModel(StereotypeDefinition stereotypeDefinition, String namespace) {
    return computeIfAbsent(stereotypesCache, stereotypeDefinition, definition -> {

      if (!isValidStereotype(stereotypeDefinition, namespace)) {
        throw new IllegalModelDefinitionException(format(
            "Stereotype '%s' defines namespace '%s' which doesn't match extension stereotype '%s'. No extension can define "
                + "stereotypes on namespaces other than its own",
            stereotypeDefinition.getName(), stereotypeDefinition.getNamespace(),
            namespace));
      }

      String resolvedNamespace = isBlank(stereotypeDefinition.getNamespace()) ? namespace : stereotypeDefinition.getNamespace();
      final StereotypeModelBuilder builder = newStereotype(stereotypeDefinition.getName(), resolvedNamespace);
      stereotypeDefinition.getParent().ifPresent(parent -> {
        String parentNamespace = parent.getNamespace();
        if (isBlank(parentNamespace)) {
          parentNamespace = namespace;
        }
        builder.withParent(toStereotypeModel(parent, parentNamespace));
      });

      return builder.build();
    });
  }

  private boolean isValidStereotype(StereotypeDefinition stereotypeDefinition, String namespace) {
    if (isBlank(stereotypeDefinition.getNamespace())) {
      return true;
    }

    return namespace.equals(stereotypeDefinition.getNamespace()) || NAMESPACE.equals(stereotypeDefinition.getNamespace());
  }

  public StereotypeModel createStereotype(String name, StereotypeModel parent) {
    return newStereotype(name, namespace).withParent(parent).build();
  }

  public StereotypeModel getValidatorStereotype() {
    return validatorStereotype.get();
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

  }

  private <T extends HasStereotypeDeclarer & HasModelProperties> StereotypeModel doAddStereotypes(
      StereotypeModelParser parser,
      T declarer,
      Optional<Supplier<StereotypeModel>> fallback) {

    ParsedStereotype stereotype = parser.getParsedStereotype();
    if (stereotype.isValidator() || stereotype.getStereotypeModel().isPresent()) {
      declarer.withModelProperty(new CustomStereotypeModelProperty());
    }

    StereotypeModel model = stereotype.getStereotypeModel().orElseGet(() -> fallback.map(Supplier::get).orElse(null));

    if (model == null && stereotype.isValidator()) {
      model = getValidatorStereotype();
    }

    if (model != null) {
      declarer.withStereotype(model);
    }

    return model;
  }

  private void addAllowedStereotypes(String namespace, ComponentDeclaration<?> declaration, MethodElement methodElement) {

    Map<String, NestableElementDeclaration<?>> nested = declaration.getNestedComponents().stream()
        .collect(toMap(NamedDeclaration::getName, n -> n));

    methodElement.getParameters().stream()
        .filter(p -> nested.containsKey(p.getAlias()))
        .forEach(parameter -> {
          if (isProcessorChain(parameter)) {
            addAllowedStereotypes(namespace, parameter, (WithAllowedStereotypesDeclaration) nested.get(parameter.getAlias()));

          } else if (isRoute(parameter)) {
            NestedRouteDeclaration route = (NestedRouteDeclaration) nested.get(parameter.getAlias());
            Optional<AllowedStereotypes> allowedStereotypes = parameter.getType().getAnnotation(AllowedStereotypes.class);
            allowedStereotypes.ifPresent(processorsStereotypes -> {
              NestableElementDeclaration processorsChain = route.getNestedComponents().stream()
                  .filter(routeChild -> routeChild instanceof NestedChainDeclaration)
                  .findFirst()
                  .orElseThrow(() -> new IllegalStateException("Missing Chain component in Route declaration"));

              addAllowedStereotypes((WithAllowedStereotypesDeclaration) processorsChain,
                  processorsStereotypes.value(), namespace);
            });
          }
        });
  }


  private void addAllowedStereotypes(String namespace, ExtensionParameter parameter,
                                     WithAllowedStereotypesDeclaration declaration) {
    Optional<AllowedStereotypes> allowedStereotypes = parameter.getAnnotation(AllowedStereotypes.class);

    if (allowedStereotypes.isPresent()) {
      addAllowedStereotypes(declaration, allowedStereotypes.get().value(), namespace);
    } else {
      declaration.addAllowedStereotype(PROCESSOR);
    }
  }


  private void addAllowedStereotypes(WithAllowedStereotypesDeclaration declaration,
                                     Class<? extends org.mule.runtime.extension.api.stereotype.StereotypeDefinition>[] stereotypes, String namespace) {
    for (Class<? extends org.mule.runtime.extension.api.stereotype.StereotypeDefinition> definition : stereotypes) {
      declaration.addAllowedStereotype(createCustomStereotype(definition, namespace, stereotypesCache));
    }
  }

  private void populateComponentConfigsMap(ConfigurationDeclaration config) {
    config.getConstructs().forEach(construct -> componentConfigs.put(construct, config));
    config.getMessageSources().forEach(source -> componentConfigs.put(source, config));
    config.getOperations().forEach(operation -> componentConfigs.put(operation, config));
  }

  private String getStereotypePrefix(ExtensionDeclarer extensionDeclarer) {
    return extensionDeclarer.getDeclaration().getXmlDslModel().getPrefix().toUpperCase();
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace.toUpperCase();
  }

  private static class DefaultParsedStereotype implements ParsedStereotype {

    private final Optional<StereotypeModel> stereotype;
    private final boolean validator;

    public DefaultParsedStereotype(Optional<StereotypeModel> stereotype, boolean validator) {
      this.stereotype = stereotype;
      this.validator = validator;
    }

    @Override
    public Optional<StereotypeModel> getStereotypeModel() {
      return stereotype;
    }

    @Override
    public boolean isValidator() {
      return validator;
    }
  }
}
