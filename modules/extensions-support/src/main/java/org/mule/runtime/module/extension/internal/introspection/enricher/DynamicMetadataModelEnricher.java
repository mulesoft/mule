/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.enricher;

import static java.lang.Thread.currentThread;
import static java.util.Optional.empty;
import static org.mule.runtime.api.meta.model.display.LayoutModel.builderFrom;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getAnnotatedElement;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.api.meta.model.declaration.fluent.BaseDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ComponentDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.TypedDeclaration;
import org.mule.runtime.core.internal.metadata.DefaultMetadataResolverFactory;
import org.mule.runtime.core.internal.metadata.NullMetadataResolverFactory;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyPart;
import org.mule.runtime.extension.api.annotation.metadata.MetadataScope;
import org.mule.runtime.extension.api.annotation.param.Query;
import org.mule.runtime.extension.api.declaration.DescribingContext;
import org.mule.runtime.extension.api.declaration.spi.ModelEnricher;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.exception.IllegalParameterModelDefinitionException;
import org.mule.runtime.extension.api.metadata.MetadataResolverFactory;
import org.mule.runtime.extension.api.model.property.MetadataKeyIdModelProperty;
import org.mule.runtime.extension.api.model.property.MetadataKeyPartModelProperty;
import org.mule.runtime.module.extension.internal.introspection.ParameterGroup;
import org.mule.runtime.module.extension.internal.metadata.MetadataScopeAdapter;
import org.mule.runtime.module.extension.internal.metadata.QueryMetadataResolverFactory;
import org.mule.runtime.module.extension.internal.model.property.ImplementingMethodModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ImplementingParameterModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.model.property.MetadataResolverFactoryModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.model.property.QueryParameterModelProperty;
import org.mule.runtime.module.extension.internal.util.IdempotentDeclarationWalker;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * {@link ModelEnricher} implementation that walks through a {@link ExtensionDeclaration} and looks for components (Operations and
 * Message sources) annotated with {@link MetadataScope} or {@link Query}. If a custom metadata scope is used, the component will
 * be considered of dynamic type.
 *
 * @since 4.0
 */
public class DynamicMetadataModelEnricher extends AbstractAnnotatedModelEnricher {

  private Class<?> extensionType;
  private ClassTypeLoader typeLoader;

  @Override
  public void enrich(DescribingContext describingContext) {
    Optional<ImplementingTypeModelProperty> implementingType =
        extractExtensionType(describingContext.getExtensionDeclarer().getDeclaration());
    if (implementingType.isPresent()) {
      extensionType = implementingType.get().getType();
      typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader(currentThread().getContextClassLoader());

      new IdempotentDeclarationWalker() {

        @Override
        public void onSource(SourceDeclaration declaration) {
          enrichSourceMetadata(declaration);
        }

        @Override
        public void onOperation(OperationDeclaration declaration) {
          enrichOperationMetadata(declaration);
        }

        @Override
        public void onParameter(ParameterDeclaration declaration) {
          enrichParameter(declaration);
        }
      }.walk(describingContext.getExtensionDeclarer().getDeclaration());
    }
  }

  private void enrichParameter(ParameterDeclaration declaration) {
    Optional<AnnotatedElement> annotatedElement = getAnnotatedElement(declaration);

    if (annotatedElement.isPresent()) {
      parseMetadataAnnotations(annotatedElement.get(), declaration);
    }
  }

  private void enrichSourceMetadata(SourceDeclaration declaration) {
    declaration.getModelProperty(ImplementingTypeModelProperty.class)
        .ifPresent(prop -> {
          final Class<?> sourceType = prop.getType();
          MetadataScopeAdapter metadataScope = new MetadataScopeAdapter(extensionType, sourceType);
          declareMetadataResolverFactory(declaration, metadataScope);
        });
  }

  private void enrichOperationMetadata(OperationDeclaration declaration) {
    declaration.getModelProperty(ImplementingMethodModelProperty.class)
        .ifPresent(prop -> {
          final Method method = prop.getMethod();

          if (method.isAnnotationPresent(Query.class)) {
            enrichWithDsql(declaration, method);
          } else {
            MetadataScopeAdapter metadataScope = new MetadataScopeAdapter(extensionType, method, declaration);
            declareMetadataResolverFactory(declaration, metadataScope);
          }
        });
  }

  private void declareMetadataResolverFactory(ComponentDeclaration<? extends ComponentDeclaration> declaration,
                                              MetadataScopeAdapter metadataScope) {
    MetadataResolverFactory metadataResolverFactory = getMetadataResolverFactory(metadataScope);
    declaration.addModelProperty(new MetadataResolverFactoryModelProperty(metadataResolverFactory));
    declareMetadataKeyId(declaration);
    declareOutputResolvers(declaration, metadataScope);
    declareInputResolvers(declaration, metadataScope);
  }

  private void enrichWithDsql(OperationDeclaration declaration, Method method) {
    Query query = method.getAnnotation(Query.class);
    declaration.addModelProperty(new MetadataResolverFactoryModelProperty(
                                                                          new QueryMetadataResolverFactory(query
                                                                              .nativeOutputResolver(), query.entityResolver())));
    addQueryModelProperties(declaration, query);
    declareDynamicType(declaration.getOutput());
    declareMetadataKeyId(declaration);
  }


  private void addQueryModelProperties(OperationDeclaration declaration, Query query) {
    ParameterDeclaration parameterDeclaration = declaration.getParameters()
        .stream()
        .filter(p -> p.getModelProperty(ImplementingParameterModelProperty.class).isPresent())
        .filter(p -> p.getModelProperty(ImplementingParameterModelProperty.class).get()
            .getParameter().isAnnotationPresent(MetadataKeyId.class))
        .findFirst()
        .orElseThrow(() -> new IllegalParameterModelDefinitionException(
                                                                        "Query operation must have a parameter annotated with @MetadataKeyId"));

    parameterDeclaration.addModelProperty(new QueryParameterModelProperty(query.translator()));
    parameterDeclaration.setLayoutModel(builderFrom(parameterDeclaration.getLayoutModel()).asQuery().build());
  }

  private MetadataResolverFactory getMetadataResolverFactory(MetadataScopeAdapter scope) {

    return scope.isCustomScope() ? new DefaultMetadataResolverFactory(scope.getKeysResolver(), scope.getInputResolvers(),
                                                                      scope.getOutputResolver(), scope.getAttributesResolver())
        : new NullMetadataResolverFactory();

  }

  private void declareInputResolvers(ComponentDeclaration<?> declaration, MetadataScopeAdapter metadataScope) {
    if (metadataScope.hasInputResolvers()) {
      Set<String> dynamicParameters = metadataScope.getInputResolvers().keySet();
      declaration.getParameters().stream()
          .filter(p -> dynamicParameters.contains(p.getName()))
          .forEach(this::declareDynamicType);
    }
  }

  private void declareOutputResolvers(ComponentDeclaration<?> declaration, MetadataScopeAdapter metadataScope) {
    if (metadataScope.hasOutputResolver()) {
      declareDynamicType(declaration.getOutput());
    }

    if (metadataScope.hasAttributesResolver()) {
      declareDynamicType(declaration.getOutputAttributes());
    }
  }

  private void declareDynamicType(TypedDeclaration component) {
    component.setType(component.getType(), true);
  }

  private void declareMetadataKeyId(ComponentDeclaration<? extends ComponentDeclaration> component) {
    Optional<MetadataKeyIdModelProperty> metadataKeyModelProperty = getMetadataKeyModelProperty(component);

    if (!metadataKeyModelProperty.isPresent() && component.getModelProperty(ParameterGroupModelProperty.class).isPresent()) {
      metadataKeyModelProperty =
          getMetadataKeyModelProperty(component.getModelProperty(ParameterGroupModelProperty.class).get().getGroups());
    }

    if (metadataKeyModelProperty.isPresent()) {
      component.addModelProperty(metadataKeyModelProperty.get());
    }
  }

  private Optional<MetadataKeyIdModelProperty> getMetadataKeyModelProperty(List<ParameterGroup> groups) {
    final Optional<ParameterGroup> keyId = groups.stream()
        .filter(g -> g.getContainer().isAnnotationPresent(MetadataKeyId.class))
        .findFirst();

    return keyId.isPresent()
        ? Optional.of(new MetadataKeyIdModelProperty(typeLoader.load(keyId.get().getType()), keyId.get().getContainerName()))
        : empty();
  }

  private Optional<MetadataKeyIdModelProperty> getMetadataKeyModelProperty(ComponentDeclaration<? extends ComponentDeclaration> component) {
    Optional<ParameterDeclaration> keyId = component.getParameters().stream()
        .filter(p -> getAnnotatedElement(p).map(element -> element.isAnnotationPresent(MetadataKeyId.class)).orElse(false))
        .findFirst();

    return keyId.isPresent() ? Optional.of(new MetadataKeyIdModelProperty(keyId.get().getType(), keyId.get().getName()))
        : empty();
  }

  /**
   * Enriches the {@link ParameterDeclarer} with a {@link MetadataKeyPartModelProperty}
   * if the parsedParameter is annotated either as {@link MetadataKeyId} or {@link MetadataKeyPart}
   *
   * @param element         the method annotated parameter parsed
   * @param baseDeclaration the {@link ParameterDeclarer} associated to the parsed parameter
   */
  private void parseMetadataAnnotations(AnnotatedElement element, BaseDeclaration baseDeclaration) {
    if (element.isAnnotationPresent(MetadataKeyId.class)) {
      baseDeclaration.addModelProperty(new MetadataKeyPartModelProperty(1));
    }

    if (element.isAnnotationPresent(MetadataKeyPart.class)) {
      MetadataKeyPart metadataKeyPart = element.getAnnotation(MetadataKeyPart.class);
      baseDeclaration.addModelProperty(new MetadataKeyPartModelProperty(metadataKeyPart.order()));
    }
  }

}
