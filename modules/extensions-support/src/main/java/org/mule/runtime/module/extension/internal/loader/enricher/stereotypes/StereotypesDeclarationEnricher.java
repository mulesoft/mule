/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher.stereotypes;

import static org.mule.runtime.api.meta.model.stereotype.StereotypeModelBuilder.newStereotype;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.core.internal.util.FunctionalUtils.ifPresent;
import static org.mule.runtime.extension.api.loader.DeclarationEnricherPhase.POST_STRUCTURE;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.CONFIG;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.CONNECTION;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.PROCESSOR;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.SOURCE;
import org.mule.metadata.api.ClassTypeLoader;
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
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.StereotypedDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.WithConstructsDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.WithOperationsDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.WithSourcesDeclaration;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.extension.api.annotation.error.ErrorTypes;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.declaration.fluent.util.IdempotentDeclarationWalker;
import org.mule.runtime.extension.api.declaration.type.DefaultExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.declaration.type.annotation.StereotypeTypeAnnotation;
import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.extension.api.loader.DeclarationEnricherPhase;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.stereotype.StereotypeDefinition;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingMethodModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionTypeDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.MethodWrapper;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.OperationWrapper;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.TypeWrapper;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * {@link DeclarationEnricher} implementation which enriches the {@link ExtensionModel} and their {@link OperationModel} from the
 * used {@link ErrorTypes} and {@link Throws} in an Annotation based extension.
 *
 * @since 4.0.0
 */
public class StereotypesDeclarationEnricher implements DeclarationEnricher {

  @Override
  public DeclarationEnricherPhase getExecutionPhase() {
    return POST_STRUCTURE;
  }

  @Override
  public void enrich(ExtensionLoadingContext extensionLoadingContext) {
    withContextClassLoader(extensionLoadingContext.getExtensionClassLoader(),
                           () -> new EnricherDelegate().apply(extensionLoadingContext));
  }

  private static class EnricherDelegate {

    private final Map<StereotypeDefinition, StereotypeModel> stereotypes = new HashMap<>();

    private String namespace;
    private StereotypeModel sourceParent;
    private StereotypeModel processorParent;
    private ClassTypeLoader typeLoader;

    public void apply(ExtensionLoadingContext extensionLoadingContext) {
      ExtensionDeclarer extensionDeclarer = extensionLoadingContext.getExtensionDeclarer();
      this.typeLoader =
          new DefaultExtensionsTypeLoaderFactory().createTypeLoader(extensionLoadingContext.getExtensionClassLoader());
      ExtensionDeclaration declaration = extensionLoadingContext.getExtensionDeclarer().getDeclaration();
      this.namespace = getStereotypePrefix(extensionDeclarer);
      this.processorParent = newStereotype(PROCESSOR.getType(), namespace).withParent(PROCESSOR).build();
      this.sourceParent = newStereotype(SOURCE.getType(), namespace).withParent(SOURCE).build();

      IdempotentDeclarationWalker enricher = declaration.getModelProperty(ImplementingTypeModelProperty.class).isPresent()
          ? getJavaBasedStereotypeEnricher()
          : getDefaultStereotypeEnricher();

      enricher.walk(declaration);

      resolveDeclaredTypesStereotypes(declaration, namespace);
    }

    private IdempotentDeclarationWalker getJavaBasedStereotypeEnricher() {
      return new IdempotentDeclarationWalker() {

        @Override
        protected void onConfiguration(ConfigurationDeclaration declaration) {
          final StereotypeModel defaultStereotype = createStereotype(declaration.getName(), CONFIG);
          ifPresent(declaration.getModelProperty(ExtensionTypeDescriptorModelProperty.class)
              .map(ExtensionTypeDescriptorModelProperty::getType),
                    type -> resolveStereotype(type, declaration, defaultStereotype),
                    () -> declaration.withStereotype(defaultStereotype));
        }

        @Override
        protected void onConnectionProvider(ConnectedDeclaration owner, ConnectionProviderDeclaration declaration) {
          final StereotypeModel defaultStereotype = createStereotype(declaration.getName(), CONNECTION);
          ifPresent(declaration.getModelProperty(ExtensionTypeDescriptorModelProperty.class)
              .map(ExtensionTypeDescriptorModelProperty::getType),
                    type -> resolveStereotype(type, declaration, defaultStereotype),
                    () -> declaration.withStereotype(defaultStereotype));
        }

        @Override
        protected void onConstruct(WithConstructsDeclaration owner, ConstructDeclaration declaration) {
          final StereotypeModel defaultStereotype = createStereotype(declaration.getName(), processorParent);
          ifPresent(declaration.getModelProperty(ImplementingMethodModelProperty.class)
              .map(ImplementingMethodModelProperty::getMethod)
              .map((Method method) -> new MethodWrapper(method, typeLoader)),
                    methodElement -> resolveStereotype(methodElement, declaration, defaultStereotype),
                    () -> declaration.withStereotype(defaultStereotype));

        }

        @Override
        public void onOperation(WithOperationsDeclaration owner, OperationDeclaration declaration) {
          final StereotypeModel defaultStereotype = createStereotype(declaration.getName(), processorParent);
          ifPresent(declaration.getModelProperty(ImplementingMethodModelProperty.class)
              .map(ImplementingMethodModelProperty::getMethod)
              .map(method -> new OperationWrapper(method, typeLoader)),
                    methodElement -> resolveStereotype(methodElement, declaration, defaultStereotype),
                    () -> declaration.withStereotype(defaultStereotype));
        }

        @Override
        protected void onSource(WithSourcesDeclaration owner, SourceDeclaration declaration) {
          final StereotypeModel defaultStereotype = createStereotype(declaration.getName(), sourceParent);
          ifPresent(declaration.getModelProperty(ImplementingTypeModelProperty.class)
              .map(ImplementingTypeModelProperty::getType)
              .map(declaringType -> new TypeWrapper(declaringType, typeLoader)),
                    type -> resolveStereotype(type, declaration, defaultStereotype),
                    () -> declaration.withStereotype(defaultStereotype));

        }

        private void resolveStereotype(Type type, StereotypedDeclaration<?> declaration, StereotypeModel fallback) {
          new ClassStereotypeResolver(type, declaration, namespace, fallback, stereotypes).resolveStereotype();
        }

        private void resolveStereotype(MethodWrapper<?> method, ComponentDeclaration<?> declaration, StereotypeModel fallback) {
          new MethodStereotypeResolver(method, declaration, namespace, fallback, stereotypes).resolveStereotype();
        }

      };
    }

    private IdempotentDeclarationWalker getDefaultStereotypeEnricher() {
      return new IdempotentDeclarationWalker() {

        @Override
        protected void onConfiguration(ConfigurationDeclaration declaration) {
          final StereotypeModel defaultStereotype = createStereotype(declaration.getName(), CONFIG);
          declaration.withStereotype(defaultStereotype);
        }

        @Override
        protected void onConnectionProvider(ConnectedDeclaration owner, ConnectionProviderDeclaration declaration) {
          final StereotypeModel defaultStereotype = createStereotype(declaration.getName(), CONNECTION);
          declaration.withStereotype(defaultStereotype);
        }

        @Override
        protected void onConstruct(WithConstructsDeclaration owner, ConstructDeclaration declaration) {
          final StereotypeModel defaultStereotype = createStereotype(declaration.getName(), processorParent);
          declaration.withStereotype(defaultStereotype);

        }

        @Override
        public void onOperation(WithOperationsDeclaration owner, OperationDeclaration declaration) {
          final StereotypeModel defaultStereotype = createStereotype(declaration.getName(), processorParent);
          declaration.withStereotype(defaultStereotype);
        }

        @Override
        protected void onSource(WithSourcesDeclaration owner, SourceDeclaration declaration) {
          final StereotypeModel defaultStereotype = createStereotype(declaration.getName(), sourceParent);
          declaration.withStereotype(defaultStereotype);
        }

      };
    }

    private StereotypeModel createStereotype(String name, StereotypeModel parent) {
      return newStereotype(name, namespace).withParent(parent).build();
    }

    private void resolveDeclaredTypesStereotypes(ExtensionDeclaration declaration, String namespace) {
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
