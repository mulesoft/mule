/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static org.mule.runtime.api.meta.model.display.LayoutModel.builderFrom;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.isASTMode;

import org.mule.runtime.api.meta.model.declaration.fluent.BaseDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ComponentDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExecutableComponentDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterizedDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.TypedDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.WithOutputDeclaration;
import org.mule.runtime.api.metadata.resolving.NamedTypeResolver;
import org.mule.runtime.api.metadata.resolving.TypeKeysResolver;
import org.mule.runtime.api.util.collection.Collectors;
import org.mule.runtime.core.internal.metadata.DefaultMetadataResolverFactory;
import org.mule.runtime.core.internal.metadata.NullMetadataResolverFactory;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyPart;
import org.mule.runtime.extension.api.annotation.metadata.MetadataScope;
import org.mule.runtime.extension.api.annotation.param.Query;
import org.mule.runtime.extension.api.declaration.fluent.util.IdempotentDeclarationWalker;
import org.mule.runtime.extension.api.exception.IllegalParameterModelDefinitionException;
import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.metadata.MetadataResolverFactory;
import org.mule.runtime.extension.api.metadata.NullMetadataResolver;
import org.mule.runtime.extension.api.property.MetadataKeyIdModelProperty;
import org.mule.runtime.extension.api.property.MetadataKeyPartModelProperty;
import org.mule.runtime.extension.api.property.TypeResolversInformationModelProperty;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.api.loader.java.type.MethodElement;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingParameterModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.MetadataResolverFactoryModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.QueryParameterModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionOperationDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionParameterDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionTypeDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.metadata.DefaultMetadataScopeAdapter;
import org.mule.runtime.module.extension.internal.metadata.MetadataScopeAdapter;
import org.mule.runtime.module.extension.internal.metadata.QueryMetadataResolverFactory;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * {@link DeclarationEnricher} implementation that walks through a {@link ExtensionDeclaration} and looks for components (Operations and
 * Message sources) annotated with {@link MetadataScope} or {@link Query}. If a custom metadata scope is used, the component will
 * be considered of dynamic type.
 *
 * @since 4.0
 */
public class DynamicMetadataDeclarationEnricher implements DeclarationEnricher {

  @Override
  public void enrich(ExtensionLoadingContext extensionLoadingContext) {
    new EnricherDelegate().enrich(extensionLoadingContext);
  }

  private static class EnricherDelegate extends AbstractAnnotatedDeclarationEnricher {

    private static final NullMetadataResolver nullMetadataResolver = new NullMetadataResolver();
    private Type extensionType;

    @Override
    public void enrich(ExtensionLoadingContext extensionLoadingContext) {
      BaseDeclaration declaration = extensionLoadingContext.getExtensionDeclarer().getDeclaration();
      //TODO MULE-14397 - Improve Dynamic Metadata Enricher to enrich without requiring Classes
      if (!isASTMode(declaration)) {
        Optional<ExtensionTypeDescriptorModelProperty> property =
            declaration.getModelProperty(ExtensionTypeDescriptorModelProperty.class);
        extensionType = property.get().getType();

        new IdempotentDeclarationWalker() {

          @Override
          public void onSource(SourceDeclaration declaration) {
            enrichSourceMetadata(declaration);
          }

          @Override
          public void onOperation(OperationDeclaration declaration) {
            enrichOperationMetadata(declaration);
          }

        }.walk(extensionLoadingContext.getExtensionDeclarer().getDeclaration());
      }
    }

    private void enrichSourceMetadata(SourceDeclaration declaration) {
      declaration.getModelProperty(ExtensionTypeDescriptorModelProperty.class)
          .ifPresent(prop -> {
            final Type sourceType = prop.getType();
            MetadataScopeAdapter metadataScope = new DefaultMetadataScopeAdapter(extensionType, sourceType, declaration);
            enrichResolversInformation(declaration, metadataScope);
          });
    }

    private void enrichOperationMetadata(OperationDeclaration declaration) {
      declaration.getModelProperty(ExtensionOperationDescriptorModelProperty.class)
          .map(ExtensionOperationDescriptorModelProperty::getOperationElement)
          .ifPresent(operation -> {
            if (operation.isAnnotatedWith(Query.class)) {
              enrichWithDsql(declaration, operation);
            } else {
              MetadataScopeAdapter metadataScope = new DefaultMetadataScopeAdapter(extensionType, operation, declaration);
              enrichResolversInformation(declaration, metadataScope);
            }
          });
    }

    private void enrichResolversInformation(ExecutableComponentDeclaration<?> declaration, MetadataScopeAdapter metadataScope) {
      final String categoryName = getCategoryName(metadataScope);
      declareResolversInformation(declaration, metadataScope, categoryName);
      declareMetadataResolverFactory(declaration, metadataScope, categoryName);
      enrichMetadataKeyParameters(declaration, metadataScope.getKeysResolver().get());
    }

    private void enrichMetadataKeyParameters(ParameterizedDeclaration<?> declaration,
                                             TypeKeysResolver typeKeysResolver) {
      declaration.getAllParameters()
          .forEach(paramDeclaration -> paramDeclaration.getModelProperty(ExtensionParameterDescriptorModelProperty.class)
              .ifPresent(modelProperty -> parseMetadataKeyAnnotations(modelProperty.getExtensionParameter(),
                                                                      paramDeclaration,
                                                                      typeKeysResolver)));
    }

    private void declareResolversInformation(ExecutableComponentDeclaration<? extends ComponentDeclaration> declaration,
                                             MetadataScopeAdapter metadataScope, String categoryName) {
      if (metadataScope.isCustomScope()) {
        Map<String, String> inputResolversByParam = metadataScope.getInputResolvers()
            .entrySet().stream()
            .collect(Collectors.toImmutableMap(Map.Entry::getKey,
                                               e -> e.getValue().get().getResolverName()));
        String outputResolver = metadataScope.getOutputResolver().get().getResolverName();
        String attributesResolver = metadataScope.getAttributesResolver().get().getResolverName();
        String keysResolver = metadataScope.getKeysResolver().get().getResolverName();

        //TODO MULE-15638 - Once Metadata API 2.0 is implemented we will know better if the resolver requires or not a connection of config.
        declaration.addModelProperty(new TypeResolversInformationModelProperty(categoryName,
                                                                               inputResolversByParam,
                                                                               outputResolver,
                                                                               attributesResolver,
                                                                               keysResolver,
                                                                               declaration.isRequiresConnection(),
                                                                               declaration.isRequiresConnection()));
      }
    }

    private void declareMetadataResolverFactory(ComponentDeclaration<? extends ComponentDeclaration> declaration,
                                                MetadataScopeAdapter metadataScope, String categoryName) {
      MetadataResolverFactory metadataResolverFactory = getMetadataResolverFactory(metadataScope);
      declaration.addModelProperty(new MetadataResolverFactoryModelProperty(() -> metadataResolverFactory));
      declareMetadataKeyId(declaration, categoryName);
      declareInputResolvers(declaration, metadataScope);
      if (declaration instanceof WithOutputDeclaration) {
        declareOutputResolvers((WithOutputDeclaration) declaration, metadataScope);
      }
    }

    private void enrichWithDsql(OperationDeclaration declaration, MethodElement method) {
      Query query = method.getAnnotation(Query.class).get();
      declaration.addModelProperty(new MetadataResolverFactoryModelProperty(() -> new QueryMetadataResolverFactory(
                                                                                                                   query
                                                                                                                       .nativeOutputResolver(),
                                                                                                                   query
                                                                                                                       .entityResolver())));

      addQueryModelProperties(declaration, query);
      declareDynamicType(declaration.getOutput());
      declareMetadataKeyId(declaration, null);
      enrichMetadataKeyParameters(declaration, nullMetadataResolver);
    }


    private void addQueryModelProperties(OperationDeclaration declaration, Query query) {
      ParameterDeclaration parameterDeclaration = declaration.getAllParameters()
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
        declaration.getAllParameters().stream()
            .filter(p -> dynamicParameters.contains(p.getName()))
            .forEach(this::declareDynamicType);
      }
    }

    private void declareOutputResolvers(WithOutputDeclaration declaration, MetadataScopeAdapter metadataScope) {
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

    private void declareMetadataKeyId(ComponentDeclaration<? extends ComponentDeclaration> component,
                                      String categoryName) {
      getMetadataKeyModelProperty(component, categoryName).ifPresent(component::addModelProperty);
    }

    private Optional<MetadataKeyIdModelProperty> getMetadataKeyModelProperty(ComponentDeclaration<? extends ComponentDeclaration> component,
                                                                             String categoryName) {
      Optional<MetadataKeyIdModelProperty> keyId = findMetadataKeyIdInGroups(component, categoryName);
      return keyId.isPresent() ? keyId : findMetadataKeyIdInParameters(component, categoryName);
    }

    private Optional<MetadataKeyIdModelProperty> findMetadataKeyIdInGroups(
                                                                           ComponentDeclaration<? extends ComponentDeclaration> component,
                                                                           String categoryName) {
      return component.getParameterGroups().stream()
          .map(group -> group.getModelProperty(ParameterGroupModelProperty.class).orElse(null))
          .filter(Objects::nonNull)
          .filter(group -> group.getDescriptor().getAnnotatedContainer().isAnnotatedWith(MetadataKeyId.class))
          .map(group -> new MetadataKeyIdModelProperty(group.getDescriptor().getMetadataType(),
                                                       group.getDescriptor().getName(), categoryName))
          .findFirst();
    }

    private Optional<MetadataKeyIdModelProperty> findMetadataKeyIdInParameters(
                                                                               ComponentDeclaration<? extends ComponentDeclaration> component,
                                                                               String categoryName) {
      return component.getParameterGroups().stream()
          .flatMap(g -> g.getParameters().stream())
          .filter(p -> getExtensionParameter(p).map(element -> element.isAnnotatedWith(MetadataKeyId.class)).orElse(false))
          .map(p -> new MetadataKeyIdModelProperty(p.getType(), p.getName(), categoryName))
          .findFirst();
    }

    private Optional<ExtensionParameter> getExtensionParameter(ParameterDeclaration parameterDeclaration) {
      return parameterDeclaration
          .getModelProperty(ExtensionParameterDescriptorModelProperty.class)
          .map(ExtensionParameterDescriptorModelProperty::getExtensionParameter);
    }

    /**
     * Enriches the {@link ParameterDeclarer} with a {@link MetadataKeyPartModelProperty}
     * if the parsedParameter is annotated either as {@link MetadataKeyId} or {@link MetadataKeyPart}
     *
     * @param element         the method annotated parameter parsed
     * @param baseDeclaration the {@link ParameterDeclarer} associated to the parsed parameter
     */
    private void parseMetadataKeyAnnotations(ExtensionParameter element, BaseDeclaration baseDeclaration,
                                             TypeKeysResolver keysResolver) {
      element.getValueFromAnnotation(MetadataKeyId.class)
          .ifPresent(valueFetcher -> {
            boolean hasKeyResolver = !(keysResolver instanceof NullMetadataResolver);
            baseDeclaration.addModelProperty(new MetadataKeyPartModelProperty(1, hasKeyResolver));
          });

      element.getValueFromAnnotation(MetadataKeyPart.class)
          .ifPresent(valueFetcher -> baseDeclaration
              .addModelProperty(new MetadataKeyPartModelProperty(valueFetcher.getNumberValue(MetadataKeyPart::order),
                                                                 valueFetcher
                                                                     .getBooleanValue(MetadataKeyPart::providedByKeyResolver))));

    }

    private String getCategoryName(MetadataScopeAdapter metadataScopeAdapter) {
      NamedTypeResolver resolver = metadataScopeAdapter.getKeysResolver().get();
      if (resolver instanceof NullMetadataResolver) {
        if (metadataScopeAdapter.hasInputResolvers()) {
          resolver = metadataScopeAdapter.getInputResolvers().values().iterator().next().get();
        } else {
          resolver = metadataScopeAdapter.getOutputResolver().get();
        }
      }

      return resolver instanceof NullMetadataResolver ? null : resolver.getCategoryName();
    }

  }

}
