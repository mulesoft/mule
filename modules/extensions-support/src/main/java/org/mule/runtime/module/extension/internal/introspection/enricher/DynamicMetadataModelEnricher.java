/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.enricher;

import static java.util.Arrays.stream;
import static org.mule.runtime.module.extension.internal.introspection.describer.MuleExtensionAnnotationParser.parseMetadataAnnotations;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getAnnotation;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.core.internal.metadata.DefaultMetadataResolverFactory;
import org.mule.runtime.core.internal.metadata.NullMetadataResolverFactory;
import org.mule.runtime.extension.api.annotation.metadata.Content;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.metadata.MetadataScope;
import org.mule.runtime.extension.api.introspection.ComponentModel;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.declaration.DescribingContext;
import org.mule.runtime.extension.api.introspection.declaration.fluent.BaseDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.OperationDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.OutputDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.SourceDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.spi.ModelEnricher;
import org.mule.runtime.extension.api.introspection.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.introspection.metadata.MetadataResolverFactory;
import org.mule.runtime.extension.api.introspection.property.MetadataKeyIdModelProperty;
import org.mule.runtime.module.extension.internal.introspection.describer.model.runtime.ParameterWrapper;
import org.mule.runtime.module.extension.internal.metadata.MetadataScopeAdapter;
import org.mule.runtime.module.extension.internal.model.property.DeclaringMemberModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ImplementingMethodModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ImplementingParameterModelProperty;
import org.mule.runtime.module.extension.internal.model.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.util.IdempotentDeclarationWalker;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Optional;

/**
 * {@link ModelEnricher} implementation that walks through a {@link ExtensionDeclaration} and looks for components (Operations and
 * Message sources) annotated with {@link MetadataScope}. If a custom metadata scope is used, the component will be considered of
 * dynamic type.
 *
 * @since 4.0
 */
public class DynamicMetadataModelEnricher extends AbstractAnnotatedModelEnricher {

  private Class<?> extensionType;
  private ClassTypeLoader typeLoader;

  @Override
  public void enrich(DescribingContext describingContext) {
    extensionType = extractExtensionType(describingContext.getExtensionDeclarer().getDeclaration());
    if (extensionType != null) {
      typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader(Thread.currentThread().getContextClassLoader());

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

  private void enrichSourceMetadata(SourceDeclaration sourceDeclaration) {
    final Optional<ImplementingTypeModelProperty> modelProperty =
        sourceDeclaration.getModelProperty(ImplementingTypeModelProperty.class);
    if (modelProperty.isPresent()) {
      final Class<?> operationMethod = modelProperty.get().getType();

      MetadataScopeAdapter metadataScope = new MetadataScopeAdapter(getMetadataScope(operationMethod));
      MetadataResolverFactory metadataResolverFactory = getMetadataResolverFactory(metadataScope);
      sourceDeclaration.setMetadataResolverFactory(metadataResolverFactory);

      if (metadataScope.isCustomScope()) {
        declareOutput(sourceDeclaration.getOutput(), sourceDeclaration.getOutputAttributes(), metadataScope);
        declareComponentMetadataKeyId(operationMethod, sourceDeclaration);
      }
    }
  }

  private void enrichOperationMetadata(OperationDeclaration operationDeclaration) {
    final Optional<ImplementingMethodModelProperty> modelProperty =
        operationDeclaration.getModelProperty(ImplementingMethodModelProperty.class);
    if (modelProperty.isPresent()) {
      final Method operationMethod = modelProperty.get().getMethod();
      MetadataScopeAdapter metadataScope = new MetadataScopeAdapter(getMetadataScope(operationMethod));
      MetadataResolverFactory metadataResolverFactory = getMetadataResolverFactory(metadataScope);
      operationDeclaration.setMetadataResolverFactory(metadataResolverFactory);

      if (metadataScope.isCustomScope()) {
        declareOutput(operationDeclaration.getOutput(), operationDeclaration.getOutputAttributes(), metadataScope);
        declareContent(operationDeclaration, operationMethod);
        declareComponentMetadataKeyId(operationMethod, operationDeclaration);
      }
    }
  }

  private void declareContent(OperationDeclaration operationDeclaration, Method operationMethod) {
    final Parameter[] parameters = operationMethod.getParameters();
    for (int i = 0; i < parameters.length; i++) {
      if (parameters[i].getAnnotation(Content.class) != null) {
        final ParameterWrapper parameterWrapper = new ParameterWrapper(operationMethod, i);
        operationDeclaration.getParameters().stream()
            .filter(parameterDeclaration -> parameterDeclaration.getName().equals(parameterWrapper.getAlias()))
            .forEach(parameterDeclaration -> parameterDeclaration.setType(parameterDeclaration.getType(), true));
      }
    }
  }

  /**
   * Checks if the {@link ComponentModel Component's} type is annotated with {@link MetadataScope}, if it doesn't then looks if
   * the {@link ExtensionModel Extension's} type is annotated.
   */
  private MetadataScope getMetadataScope(Class<?> componentClass) {
    MetadataScope scope = getAnnotation(componentClass, MetadataScope.class);
    return scope != null ? scope : getAnnotation(extensionType, MetadataScope.class);
  }

  /**
   * Checks if the method is annotated with {@link MetadataScope}, if not looks whether the operation class containing the method
   * is annotated or not. And lastly, if no annotation was found so far, checks if the extension class is annotated.
   */
  private MetadataScope getMetadataScope(Method method) {
    MetadataScope scope = method.getAnnotation(MetadataScope.class);
    return scope != null ? scope : getMetadataScope(method.getDeclaringClass());
  }

  private MetadataResolverFactory getMetadataResolverFactory(MetadataScopeAdapter scope) {
    return scope.isCustomScope() ? new DefaultMetadataResolverFactory(scope.getKeysResolver(), scope.getContentResolver(),
                                                                      scope.getOutputResolver(), scope.getAttributesResolver())
        : new NullMetadataResolverFactory();

  }

  private void declareOutput(OutputDeclaration outputDeclaration, OutputDeclaration attributesDeclaration,
                             MetadataScopeAdapter metadataScope) {
    if (metadataScope.hasOutputResolver()) {
      declareOutputType(outputDeclaration);
    }

    if (metadataScope.hasAttributesResolver()) {
      declareOutputType(attributesDeclaration);
    }
  }

  private void declareOutputType(OutputDeclaration component) {
    component.setType(component.getType(), true);
  }

  private void declareComponentMetadataKeyId(Method method, BaseDeclaration operation) {
    stream(method.getParameters()).filter(p -> p.isAnnotationPresent(MetadataKeyId.class)).findFirst()
        .ifPresent(p -> operation.addModelProperty(new MetadataKeyIdModelProperty(typeLoader.load(p.getType()))));
  }

  private void declareComponentMetadataKeyId(Class<?> clazz, BaseDeclaration operation) {
    stream(clazz.getDeclaredFields()).filter(p -> p.isAnnotationPresent(MetadataKeyId.class)).findFirst()
        .ifPresent(p -> operation.addModelProperty(new MetadataKeyIdModelProperty(typeLoader.load(p.getType()))));
  }

  private Optional<AnnotatedElement> getAnnotatedElement(ParameterDeclaration declaration) {
    final Optional<DeclaringMemberModelProperty> declaringMemberProperty =
        declaration.getModelProperty(DeclaringMemberModelProperty.class);
    final Optional<ImplementingParameterModelProperty> implementingParameterProperty =
        declaration.getModelProperty(ImplementingParameterModelProperty.class);
    AnnotatedElement annotatedElement = null;

    if (declaringMemberProperty.isPresent()) {
      annotatedElement = declaringMemberProperty.get().getDeclaringField();
    }

    if (implementingParameterProperty.isPresent()) {
      annotatedElement = implementingParameterProperty.get().getParameter();
    }

    return Optional.ofNullable(annotatedElement);
  }
}
