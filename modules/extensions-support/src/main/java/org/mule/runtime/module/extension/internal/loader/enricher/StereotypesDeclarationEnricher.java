/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static org.mule.runtime.api.meta.model.stereotype.StereotypeModelBuilder.newStereotype;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getAnnotation;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ComponentDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.NestedChainDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.WithOperationsDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.WithSourcesDeclaration;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModelBuilder;
import org.mule.runtime.core.api.util.ClassUtils;
import org.mule.runtime.extension.api.annotation.error.ErrorTypes;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.param.stereotype.AllowedStereotypes;
import org.mule.runtime.extension.api.annotation.param.stereotype.Stereotype;
import org.mule.runtime.extension.api.declaration.fluent.util.IdempotentDeclarationWalker;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.runtime.process.Chain;
import org.mule.runtime.extension.api.stereotype.MuleStereotypeDefinition;
import org.mule.runtime.extension.api.stereotype.MuleStereotypeFactory;
import org.mule.runtime.extension.api.stereotype.StereotypeDefinition;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingMethodModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.MethodWrapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * {@link DeclarationEnricher} implementation which enriches the {@link ExtensionModel} and their {@link OperationModel} from the
 * used {@link ErrorTypes} and {@link Throws} in an Annotation based extension.
 *
 * @since 4.0
 */
public class StereotypesDeclarationEnricher implements DeclarationEnricher {

  @Override
  public void enrich(ExtensionLoadingContext extensionLoadingContext) {
    new EnricherDelegate().apply(extensionLoadingContext);
  }

  private static class EnricherDelegate {

    private final Map<StereotypeDefinition, StereotypeModel> stereotypes = new HashMap<>();

    public void apply(ExtensionLoadingContext extensionLoadingContext) {
      ExtensionDeclarer extensionDeclarer = extensionLoadingContext.getExtensionDeclarer();
      ExtensionDeclaration declaration = extensionLoadingContext.getExtensionDeclarer().getDeclaration();
      Optional<ImplementingTypeModelProperty> implementingType =
          declaration.getModelProperty(ImplementingTypeModelProperty.class);

      if (implementingType.isPresent()) {
        new IdempotentDeclarationWalker() {

          @Override
          public void onOperation(WithOperationsDeclaration owner, OperationDeclaration declaration) {
            declaration.getModelProperty(ImplementingMethodModelProperty.class)
                .map(ImplementingMethodModelProperty::getMethod)
                .map(MethodWrapper::new)
                .ifPresent(methodElement -> addStereotypes(extensionDeclarer,
                                                           methodElement,
                                                           declaration,
                                                           MuleStereotypeFactory.processor()));
          }

          @Override
          protected void onSource(WithSourcesDeclaration owner, SourceDeclaration declaration) {
            declaration.getModelProperty(ImplementingTypeModelProperty.class)
                .map(ImplementingTypeModelProperty::getType)
                .ifPresent(declaringType -> addStereotypes(extensionDeclarer,
                                                           declaringType,
                                                           declaration,
                                                           MuleStereotypeFactory.source()));

          }
        }.walk(declaration);
      }
    }

    void addStereotypes(ExtensionDeclarer extensionDeclarer, MethodWrapper methodElement,
                        ComponentDeclaration declaration, StereotypeModel defaultStereotype) {

      Stereotype stereotypes = methodElement.getMethod().getAnnotation(Stereotype.class);
      if (stereotypes == null) {
        addStereotypes(extensionDeclarer, methodElement.getDeclaringClass(), declaration, defaultStereotype);
      } else {
        addStereotypes(extensionDeclarer, declaration, stereotypes, defaultStereotype);
      }

      methodElement.getParameters().stream()
          .filter(p -> Chain.class.equals(p.getType().getDeclaringClass()))
          .findFirst()
          .ifPresent(param -> declaration.getNestedComponents().stream()
              .filter(NestedChainDeclaration.class::isInstance)
              .findFirst()
              .ifPresent(model -> addAllowedStereotypes(extensionDeclarer, param, (NestedChainDeclaration) model)));
    }

    void addStereotypes(ExtensionDeclarer extensionDeclarer, Class<?> annotatedClass,
                        ComponentDeclaration declaration, StereotypeModel defaultStereotype) {
      addStereotypes(extensionDeclarer, declaration, getAnnotation(annotatedClass, Stereotype.class),
                     defaultStereotype);
    }

    private void addStereotypes(ExtensionDeclarer extensionDeclarer, ComponentDeclaration declaration,
                                Stereotype customStereotype, StereotypeModel defaultStereotype) {

      if (customStereotype != null) {
        final String extensionPrefix = extensionDeclarer.getDeclaration().getXmlDslModel().getPrefix().toUpperCase();
        try {
          declaration.withStereotype(getStereotype(ClassUtils.instantiateClass(customStereotype.value()), extensionPrefix));
        } catch (Exception e) {
          throw new IllegalModelDefinitionException("Invalid StereotypeDefinition found with name: "
              + customStereotype.value().getName(), e);
        }
      } else {
        declaration.withStereotype(defaultStereotype);
      }
    }

    StereotypeModel getStereotype(StereotypeDefinition stereotypeDefinition, String namespace) {
      return stereotypes.computeIfAbsent(stereotypeDefinition, definition -> {
        final StereotypeModelBuilder builder = newStereotype(stereotypeDefinition.getName(), namespace);
        stereotypeDefinition.getParent().ifPresent(parent -> {
          String parentNamespace = parent instanceof MuleStereotypeDefinition ? "MULE" : namespace;
          builder.withParent(newStereotype(parent.getName(), parentNamespace).build());
        });

        return builder.build();
      });
    }

    private void addAllowedStereotypes(ExtensionDeclarer extensionDeclarer, ExtensionParameter parameter,
                                       NestedChainDeclaration declaration) {
      Optional<AllowedStereotypes> stereotypes = parameter.getAnnotation(AllowedStereotypes.class);

      if (stereotypes.isPresent()) {
        final String extensionPrefix = extensionDeclarer.getDeclaration().getXmlDslModel().getPrefix().toUpperCase();
        for (Class<? extends StereotypeDefinition> definition : stereotypes.get().value()) {
          try {
            declaration.addAllowedStereotype(getStereotype(ClassUtils.instantiateClass(definition), extensionPrefix));
          } catch (Exception e) {
            throw new IllegalModelDefinitionException("Invalid StereotypeDefinition found with name " + definition.getName(), e);
          }
        }
      } else {
        declaration.addAllowedStereotype(MuleStereotypeFactory.processor());
      }
    }

  }
}
