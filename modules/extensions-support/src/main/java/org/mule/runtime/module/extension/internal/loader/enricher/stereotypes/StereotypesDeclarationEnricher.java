/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher.stereotypes;

import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;
import static org.mule.runtime.api.meta.model.stereotype.StereotypeModelBuilder.newStereotype;
import static org.mule.runtime.core.api.util.ClassUtils.instantiateClass;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.core.api.util.StringUtils.isBlank;
import static org.mule.runtime.core.internal.util.FunctionalUtils.ifPresent;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypeDefinition.NAMESPACE;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.CONFIG;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.CONNECTION;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.PROCESSOR;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.SOURCE;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.VALIDATOR_DEFINITION;
import static org.mule.runtime.module.extension.internal.loader.utils.ModelLoaderUtils.isProcessorChain;
import static org.mule.runtime.module.extension.internal.loader.utils.ModelLoaderUtils.isRoute;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.IntersectionType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.UnionType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ComponentDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConnectedDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConnectionProviderDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConstructDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.NamedDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.NestableElementDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.NestedChainDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.NestedRouteDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.WithAllowedStereotypesDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.WithConstructsDeclaration;
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
import org.mule.runtime.extension.api.annotation.param.stereotype.Validator;
import org.mule.runtime.extension.api.declaration.fluent.util.IdempotentDeclarationWalker;
import org.mule.runtime.extension.api.declaration.type.annotation.StereotypeTypeAnnotation;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.stereotype.MuleStereotypes;
import org.mule.runtime.extension.api.stereotype.StereotypeDefinition;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingMethodModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.internal.loader.java.type.MethodElement;
import org.mule.runtime.module.extension.internal.loader.java.type.Type;
import org.mule.runtime.module.extension.internal.loader.java.type.WithAnnotations;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.MethodWrapper;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.TypeWrapper;
import org.mule.runtime.module.extension.internal.util.IntrospectionUtils;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
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

      if (implementingType.isPresent()) {
        new IdempotentDeclarationWalker() {

          @Override
          protected void onConfiguration(ConfigurationDeclaration declaration) {
            final StereotypeModel defaultConfigStereotype = newStereotype(declaration.getName(), namespace)
                .withParent(CONFIG).build();
            ifPresent(
                      declaration.getModelProperty(ImplementingTypeModelProperty.class)
                          .map(ImplementingTypeModelProperty::getType),
                      declaringType -> new ClassStereotypeResolver(new TypeWrapper(declaringType), declaration, namespace,
                                                                   defaultConfigStereotype, stereotypes).resolveStereotype(),
                      () -> declaration.withStereotype(defaultConfigStereotype));
          }

          @Override
          protected void onConnectionProvider(ConnectedDeclaration owner, ConnectionProviderDeclaration declaration) {
            final StereotypeModel defaultConnectionStereotype = newStereotype(declaration.getName(), namespace)
                .withParent(CONNECTION).build();
            ifPresent(
                      declaration.getModelProperty(ImplementingTypeModelProperty.class)
                          .map(ImplementingTypeModelProperty::getType),
                      declaringType -> new ClassStereotypeResolver(new TypeWrapper(declaringType), declaration, namespace,
                                                                   defaultConnectionStereotype, stereotypes).resolveStereotype(),
                      () -> declaration.withStereotype(defaultConnectionStereotype));
          }

          @Override
          protected void onConstruct(WithConstructsDeclaration owner, ConstructDeclaration declaration) {
            declaration.getModelProperty(ImplementingMethodModelProperty.class)
                .map(ImplementingMethodModelProperty::getMethod)
                .map(MethodWrapper::new)
                .ifPresent(methodElement -> new MethodStereotypeResolver(methodElement, declaration, namespace, PROCESSOR,
                                                                         stereotypes)
                                                                             .resolveStereotype());
          }

          @Override
          public void onOperation(WithOperationsDeclaration owner, OperationDeclaration declaration) {
            declaration.getModelProperty(ImplementingMethodModelProperty.class)
                .map(ImplementingMethodModelProperty::getMethod)
                .map(MethodWrapper::new)
                .ifPresent(methodElement -> new MethodStereotypeResolver(methodElement, declaration, namespace, PROCESSOR,
                                                                         stereotypes)
                                                                             .resolveStereotype());
          }

          @Override
          protected void onSource(WithSourcesDeclaration owner, SourceDeclaration declaration) {
            declaration.getModelProperty(ImplementingTypeModelProperty.class)
                .map(ImplementingTypeModelProperty::getType)
                .ifPresent(declaringType -> new ClassStereotypeResolver(new TypeWrapper(declaringType), declaration, namespace,
                                                                        SOURCE,
                                                                        stereotypes)
                                                                            .resolveStereotype());

          }
        }.walk(declaration);
      }

      resolveStereotypes(declaration, namespace);
    }

    private void resolveStereotypes(ExtensionDeclaration declaration, String namespace) {
      Function<Class<? extends StereotypeDefinition>, StereotypeModel> resolver =
          def -> StereotypeResolver.createCustomStereotype(def, namespace, stereotypes);
      declaration.getTypes().forEach(type -> resolveStereotype(type, resolver));
    }

    private void resolveStereotype(ObjectType type, Function<Class<? extends StereotypeDefinition>, StereotypeModel> resolver) {
      type.accept(new MetadataTypeVisitor() {

        // This is created to avoid a recursive types infinite loop, producing an StackOverflow when resolving the stereotypes.
        private List<MetadataType> registeredTypes = new LinkedList<>();

        @Override
        public void visitObject(ObjectType objectType) {
          if (!registeredTypes.contains(objectType)) {
            registeredTypes.add(objectType);
            objectType.getAnnotation(StereotypeTypeAnnotation.class).ifPresent(a -> a.resolveStereotypes(resolver));
            objectType.getFields().forEach(f -> f.getValue().accept(this));
          }
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

    private String getStereotypePrefix(ExtensionDeclarer extensionDeclarer) {
      return extensionDeclarer.getDeclaration().getXmlDslModel().getPrefix().toUpperCase();
    }
  }
}
