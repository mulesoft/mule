/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.extension.internal.loader.enricher.stereotypes;

import static java.util.stream.Collectors.toMap;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.PROCESSOR;
import static org.mule.runtime.module.extension.internal.loader.utils.ModelLoaderUtils.isProcessorChain;
import static org.mule.runtime.module.extension.internal.loader.utils.ModelLoaderUtils.isRoute;

import org.mule.runtime.api.meta.model.declaration.fluent.ComponentDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.NamedDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.NestableElementDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.NestedChainDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.NestedRouteDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.WithAllowedStereotypesDeclaration;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.extension.api.annotation.param.stereotype.AllowedStereotypes;
import org.mule.runtime.extension.api.stereotype.StereotypeDefinition;
import org.mule.runtime.module.extension.internal.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.internal.loader.java.type.MethodElement;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.MethodWrapper;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.TypeWrapper;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Optional;

/**
 * {@link StereotypeResolver} implementation for java methods.
 *
 * @since 4.0
 */
class MethodStereotypeResolver extends StereotypeResolver<MethodElement> {


  private final MethodWrapper methodElement;

  public MethodStereotypeResolver(MethodWrapper methodElement,
                                  ComponentDeclaration declaration,
                                  String namespace,
                                  StereotypeModel fallbackStereotype,
                                  Map<StereotypeDefinition, StereotypeModel> stereotypesCache) {
    super(methodElement, declaration, namespace, fallbackStereotype, stereotypesCache);
    this.methodElement = methodElement;
  }

  @Override
  protected void resolveStereotype() {
    super.resolveStereotype();

    addAllowedStereotypes(namespace, (ComponentDeclaration<?>) declaration, methodElement);
  }

  @Override
  protected void addFallbackStereotype() {
    new ClassStereotypeResolver(new TypeWrapper(annotatedElement.getDeclaringClass()),
                                declaration,
                                namespace,
                                fallbackStereotype,
                                stereotypesCache).resolveStereotype();
  }

  private void addAllowedStereotypes(String namespace, ComponentDeclaration<?> declaration, MethodWrapper methodElement) {

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

  private void addAllowedStereotypes(WithAllowedStereotypesDeclaration declaration,
                                     Class<? extends StereotypeDefinition>[] stereotypes, String namespace) {
    for (Class<? extends StereotypeDefinition> definition : stereotypes) {
      declaration.addAllowedStereotype(createCustomStereotype(definition, namespace, stereotypesCache));
    }
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

  @Override
  protected <T extends Annotation> T getAnnotation(Class<T> annotationType) {
    return annotatedElement.getAnnotation(annotationType).orElse(null);
  }

  @Override
  protected String resolveDescription() {
    return "Method '" + annotatedElement.getName() + "'";
  }
}

