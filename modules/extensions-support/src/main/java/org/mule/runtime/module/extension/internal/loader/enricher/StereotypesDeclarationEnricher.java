/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static org.mule.runtime.api.meta.model.stereotype.StereotypeModelBuilder.newStereotype;
import static org.mule.runtime.core.api.util.ClassUtils.instantiateClass;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.CONFIG;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.CONNECTION;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.PROCESSOR;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.SOURCE;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getAnnotation;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.IntersectionType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.UnionType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ComponentDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConnectedDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConnectionProviderDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.NestedChainDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.WithOperationsDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.WithSourcesDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.WithStereotypesDeclaration;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModelBuilder;
import org.mule.runtime.extension.api.annotation.error.ErrorTypes;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.param.stereotype.AllowedStereotypes;
import org.mule.runtime.extension.api.annotation.param.stereotype.Stereotype;
import org.mule.runtime.extension.api.declaration.fluent.util.IdempotentDeclarationWalker;
import org.mule.runtime.extension.api.declaration.type.annotation.StereotypeTypeAnnotation;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.runtime.process.Chain;
import org.mule.runtime.extension.api.stereotype.MuleStereotypeDefinition;
import org.mule.runtime.extension.api.stereotype.StereotypeDefinition;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingMethodModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.MethodWrapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * {@link DeclarationEnricher} implementation which enriches the {@link ExtensionModel} and their {@link OperationModel} from the
 * used {@link ErrorTypes} and {@link Throws} in an Annotation based extension.
 *
 * @since 4.0
 */
public class StereotypesDeclarationEnricher implements DeclarationEnricher {

  @Override
  public void enrich(ExtensionLoadingContext extensionLoadingContext) {
    withContextClassLoader(extensionLoadingContext.getExtensionClassLoader(),
                           () -> new EnricherDelegate().apply(extensionLoadingContext));
  }

  private static class EnricherDelegate {

    private final Map<StereotypeDefinition, StereotypeModel> stereotypes = new HashMap<>();

    public void apply(ExtensionLoadingContext extensionLoadingContext) {
      ExtensionDeclarer extensionDeclarer = extensionLoadingContext.getExtensionDeclarer();
      ExtensionDeclaration declaration = extensionLoadingContext.getExtensionDeclarer().getDeclaration();
      Optional<ImplementingTypeModelProperty> implementingType =
          declaration.getModelProperty(ImplementingTypeModelProperty.class);

      final String namespace = getStereotypePrefix(extensionDeclarer);
      final StereotypeModel defaultConfigStereotype = newStereotype("CONFIG", namespace)
          .withParent(CONFIG).build();
      final StereotypeModel defaultConnectionStereotype = newStereotype("CONNECTION", namespace)
          .withParent(CONNECTION).build();

      if (implementingType.isPresent()) {
        new IdempotentDeclarationWalker() {

          @Override
          protected void onConfiguration(ConfigurationDeclaration declaration) {
            declaration.getModelProperty(ImplementingTypeModelProperty.class)
                .map(ImplementingTypeModelProperty::getType)
                .ifPresent(declaringType -> addStereotypes(namespace,
                                                           declaringType,
                                                           declaration,
                                                           defaultConfigStereotype));
          }

          @Override
          protected void onConnectionProvider(ConnectedDeclaration owner, ConnectionProviderDeclaration declaration) {
            declaration.getModelProperty(ImplementingTypeModelProperty.class)
                .map(ImplementingTypeModelProperty::getType)
                .ifPresent(declaringType -> addStereotypes(namespace,
                                                           declaringType,
                                                           declaration,
                                                           defaultConnectionStereotype));
          }

          @Override
          public void onOperation(WithOperationsDeclaration owner, OperationDeclaration declaration) {
            declaration.getModelProperty(ImplementingMethodModelProperty.class)
                .map(ImplementingMethodModelProperty::getMethod)
                .map(MethodWrapper::new)
                .ifPresent(methodElement -> addStereotypes(namespace,
                                                           methodElement,
                                                           declaration,
                                                           PROCESSOR));
          }

          @Override
          protected void onSource(WithSourcesDeclaration owner, SourceDeclaration declaration) {
            declaration.getModelProperty(ImplementingTypeModelProperty.class)
                .map(ImplementingTypeModelProperty::getType)
                .ifPresent(declaringType -> addStereotypes(namespace,
                                                           declaringType,
                                                           declaration,
                                                           SOURCE));

          }
        }.walk(declaration);
      }

      resolveStereotypes(declaration, namespace);
    }

    private void resolveStereotypes(ExtensionDeclaration declaration, String namespace) {
      Function<Class<? extends StereotypeDefinition>, StereotypeModel> resolver = def -> getStereotype(def, namespace);
      declaration.getTypes().forEach(type -> resolveStereotype(type, resolver));
    }

    private void resolveStereotype(ObjectType type, Function<Class<? extends StereotypeDefinition>, StereotypeModel> resolver) {
      type.accept(new MetadataTypeVisitor() {

        @Override
        public void visitObject(ObjectType objectType) {
          objectType.getAnnotation(StereotypeTypeAnnotation.class).ifPresent(a -> a.resolveStereotype(resolver));
          objectType.getFields().forEach(f -> f.getValue().accept(this));
        }

        @Override
        public void visitArrayType(ArrayType arrayType) {
          arrayType.getType().accept(this);
        }

        @Override
        public void visitUnion(UnionType unionType) {
          unionType.getTypes().forEach(t -> t.accept(this));
        }

        @Override
        public void visitIntersection(IntersectionType intersectionType) {
          intersectionType.getTypes().forEach(t -> t.accept(this));
        }
      });
    }

    private void addStereotypes(String namespace, MethodWrapper methodElement,
                                ComponentDeclaration declaration, StereotypeModel defaultStereotype) {

      Stereotype stereotypes = methodElement.getMethod().getAnnotation(Stereotype.class);
      if (stereotypes == null) {
        addStereotypes(namespace, methodElement.getDeclaringClass(), declaration, defaultStereotype);
      } else {
        addStereotypes(namespace, declaration, stereotypes, defaultStereotype);
      }

      methodElement.getParameters().stream()
          .filter(p -> Chain.class.equals(p.getType().getDeclaringClass()))
          .findFirst()
          .ifPresent(param -> declaration.getNestedComponents().stream()
              .filter(NestedChainDeclaration.class::isInstance)
              .findFirst()
              .ifPresent(model -> addAllowedStereotypes(namespace, param, (NestedChainDeclaration) model)));
    }

    void addStereotypes(String namespace, Class<?> annotatedClass,
                        WithStereotypesDeclaration declaration, StereotypeModel defaultStereotype) {
      addStereotypes(namespace, declaration, getAnnotation(annotatedClass, Stereotype.class),
                     defaultStereotype);
    }

    private void addStereotypes(String namespace, WithStereotypesDeclaration declaration,
                                Stereotype customStereotype, StereotypeModel defaultStereotype) {

      if (customStereotype != null) {
        declaration.withStereotype(getStereotype(customStereotype.value(), namespace));
      } else {
        declaration.withStereotype(defaultStereotype);
      }
    }

    private StereotypeModel getStereotype(Class<? extends StereotypeDefinition> definitionClass, String namespace) {
      try {
        return getStereotype(instantiateClass(definitionClass), namespace);
      } catch (Exception e) {
        throw new IllegalModelDefinitionException("Invalid StereotypeDefinition found with name: "
                                                      + definitionClass.getName(), e);
      }
    }

    private String getStereotypePrefix(ExtensionDeclarer extensionDeclarer) {
      return extensionDeclarer.getDeclaration().getXmlDslModel().getPrefix().toUpperCase();
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

    private void addAllowedStereotypes(String namespace, ExtensionParameter parameter,
                                       NestedChainDeclaration declaration) {
      Optional<AllowedStereotypes> stereotypes = parameter.getAnnotation(AllowedStereotypes.class);

      if (stereotypes.isPresent()) {
        for (Class<? extends StereotypeDefinition> definition : stereotypes.get().value()) {
          declaration.addAllowedStereotype(getStereotype(definition, namespace));
        }
      } else {
        declaration.addAllowedStereotype(PROCESSOR);
      }
    }

  }
}
