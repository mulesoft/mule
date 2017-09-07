/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static java.lang.String.format;
import static org.mule.runtime.api.meta.model.stereotype.StereotypeModelBuilder.newStereotype;
import static org.mule.runtime.core.api.util.ClassUtils.instantiateClass;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.CONFIG;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.CONNECTION;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.PROCESSOR;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.SOURCE;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.VALIDATOR_DEFINITION;
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
import org.mule.runtime.extension.api.annotation.param.stereotype.Validator;
import org.mule.runtime.extension.api.declaration.fluent.util.IdempotentDeclarationWalker;
import org.mule.runtime.extension.api.declaration.type.annotation.StereotypeTypeAnnotation;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.runtime.process.Chain;
import org.mule.runtime.extension.api.stereotype.MuleStereotypeDefinition;
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
                .ifPresent(declaringType -> new ClassStereotypeResolver(new TypeWrapper(declaringType), declaration, namespace,
                                                                        defaultConfigStereotype, stereotypes)
                                                                            .resolveStereotype());
          }

          @Override
          protected void onConnectionProvider(ConnectedDeclaration owner, ConnectionProviderDeclaration declaration) {
            declaration.getModelProperty(ImplementingTypeModelProperty.class)
                .map(ImplementingTypeModelProperty::getType)
                .ifPresent(declaringType -> new ClassStereotypeResolver(new TypeWrapper(declaringType), declaration, namespace,
                                                                        defaultConnectionStereotype, stereotypes)
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



    private String getStereotypePrefix(ExtensionDeclarer extensionDeclarer) {
      return extensionDeclarer.getDeclaration().getXmlDslModel().getPrefix().toUpperCase();
    }
  }


  private static abstract class StereotypeResolver<T extends WithAnnotations> {

    protected final T annotatedElement;
    protected final WithStereotypesDeclaration declaration;
    protected final String namespace;
    protected final StereotypeModel fallbackStereotype;
    protected Validator validatorAnnotation;
    protected Stereotype stereotypeAnnotation;
    protected Map<StereotypeDefinition, StereotypeModel> stereotypesCache;

    protected static StereotypeModel createCustomStereotype(Class<? extends StereotypeDefinition> definitionClass,
                                                            String namespace,
                                                            Map<StereotypeDefinition, StereotypeModel> stereotypesCache) {
      try {
        return getStereotype(instantiateClass(definitionClass), namespace, stereotypesCache);
      } catch (Exception e) {
        throw new IllegalModelDefinitionException(
                                                  "Invalid StereotypeDefinition found with name: " + definitionClass.getName(),
                                                  e);
      }
    }

    protected static StereotypeModel getStereotype(StereotypeDefinition stereotypeDefinition,
                                                   String namespace,
                                                   Map<StereotypeDefinition, StereotypeModel> stereotypesCache) {
      return stereotypesCache.computeIfAbsent(stereotypeDefinition, definition -> {
        final StereotypeModelBuilder builder = newStereotype(stereotypeDefinition.getName(), namespace);
        stereotypeDefinition.getParent().ifPresent(parent -> {
          String parentNamespace = parent instanceof MuleStereotypeDefinition ? "MULE" : namespace;
          builder.withParent(newStereotype(parent.getName(), parentNamespace).build());
        });

        return builder.build();
      });
    }

    protected StereotypeResolver(T annotatedElement,
                                 WithStereotypesDeclaration declaration,
                                 String namespace,
                                 StereotypeModel fallbackStereotype,
                                 Map<StereotypeDefinition, StereotypeModel> stereotypesCache) {
      this.annotatedElement = annotatedElement;
      this.declaration = declaration;
      this.namespace = namespace;
      this.stereotypesCache = stereotypesCache;
      this.fallbackStereotype = fallbackStereotype;
      stereotypeAnnotation = getAnnotation(Stereotype.class);
      validatorAnnotation = getAnnotation(Validator.class);

      if (validatorAnnotation != null && stereotypeAnnotation != null) {
        throw new IllegalModelDefinitionException(format("%s is annotated with both @%s and @%s. Only one can "
            + "be provided at the same time for the same component",
                                                         resolveDescription(), Stereotype.class.getSimpleName(),
                                                         Validator.class.getSimpleName()));
      }
    }

    protected abstract <T extends Annotation> T getAnnotation(Class<T> annotationType);

    protected abstract String resolveDescription();

    protected void resolveStereotype() {
      if (validatorAnnotation != null) {
        addValidationStereotype();
      } else if (stereotypeAnnotation != null) {
        declaration.withStereotype(createCustomStereotype());
      } else {
        addFallbackStereotype();
      }
    }

    protected void addFallbackStereotype() {
      declaration.withStereotype(fallbackStereotype);
    }

    protected StereotypeModel createCustomStereotype() {
      return createCustomStereotype(stereotypeAnnotation.value(), namespace, stereotypesCache);
    }

    protected void addValidationStereotype() {
      declaration.withStereotype(newStereotype(VALIDATOR_DEFINITION.getName(), namespace)
          .withParent(MuleStereotypes.VALIDATOR)
          .build());
    }
  }


  private static class MethodStereotypeResolver extends StereotypeResolver<MethodElement> {

    public MethodStereotypeResolver(MethodElement annotatedElement,
                                    WithStereotypesDeclaration declaration,
                                    String namespace,
                                    StereotypeModel fallbackStereotype,
                                    Map<StereotypeDefinition, StereotypeModel> stereotypesCache) {
      super(annotatedElement, declaration, namespace, fallbackStereotype, stereotypesCache);
    }

    @Override
    protected void resolveStereotype() {
      super.resolveStereotype();

      annotatedElement.getParameters().stream()
          .filter(p -> Chain.class.equals(p.getType().getDeclaringClass()))
          .findFirst()
          .ifPresent(param -> ((ComponentDeclaration) declaration).getNestedComponents().stream()
              .filter(NestedChainDeclaration.class::isInstance)
              .findFirst()
              .ifPresent(model -> addAllowedStereotypes(param, (NestedChainDeclaration) model)));
    }

    @Override
    protected void addFallbackStereotype() {
      new ClassStereotypeResolver(new TypeWrapper(annotatedElement.getDeclaringClass()),
                                  declaration,
                                  namespace,
                                  fallbackStereotype,
                                  stereotypesCache).resolveStereotype();
    }

    private void addAllowedStereotypes(ExtensionParameter parameter,
                                       NestedChainDeclaration declaration) {
      Optional<AllowedStereotypes> allowedStereotypes = parameter.getAnnotation(AllowedStereotypes.class);

      if (allowedStereotypes.isPresent()) {
        for (Class<? extends StereotypeDefinition> definitionClass : allowedStereotypes.get().value()) {
          declaration.addAllowedStereotype(createCustomStereotype(definitionClass, namespace, stereotypesCache));
        }
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


  private static class ClassStereotypeResolver extends StereotypeResolver<Type> {

    public ClassStereotypeResolver(Type annotatedElement,
                                   WithStereotypesDeclaration declaration,
                                   String namespace,
                                   StereotypeModel fallbackStereotype,
                                   Map<StereotypeDefinition, StereotypeModel> stereotypesCache) {
      super(annotatedElement, declaration, namespace, fallbackStereotype, stereotypesCache);
    }

    @Override
    protected <T extends Annotation> T getAnnotation(Class<T> annotationType) {
      return annotatedElement.getAnnotation(annotationType).orElseGet(() -> {
        Class<?> declaringClass = annotatedElement.getDeclaringClass();
        if (declaringClass != null) {
          return IntrospectionUtils.getAnnotation(declaringClass, annotationType);
        }

        return null;
      });
    }

    @Override
    protected String resolveDescription() {
      return "Class '" + annotatedElement.getName() + "'";
    }
  }
}
