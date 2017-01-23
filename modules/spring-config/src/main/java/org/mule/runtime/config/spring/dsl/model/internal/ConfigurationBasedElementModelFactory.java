/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.model.internal;

import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Stream.concat;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.operation.HasOperationModels;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.source.HasSourceModels;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.meta.model.util.ExtensionWalker;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.config.spring.dsl.model.DslElementModel;
import org.mule.runtime.config.spring.dsl.model.DslElementModelFactory;
import org.mule.runtime.dsl.api.component.config.ComponentConfiguration;
import org.mule.runtime.dsl.api.component.config.ComponentIdentifier;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Default implementation of a {@link DslElementModelFactory}
 *
 * @since 1.0
 */
class ConfigurationBasedElementModelFactory {

  private Map<ExtensionModel, DslSyntaxResolver> resolvers;
  private ExtensionModel currentExtension;
  private DslSyntaxResolver dsl;

  ConfigurationBasedElementModelFactory(Map<ExtensionModel, DslSyntaxResolver> resolvers) {
    this.resolvers = resolvers;
  }

  public <T> Optional<DslElementModel<T>> create(ComponentConfiguration configuration) {
    return Optional.ofNullable(createIdentifiedElement(configuration));
  }

  private DslElementModel createIdentifiedElement(ComponentConfiguration configuration) {

    final ComponentIdentifier identifier = configuration.getIdentifier();

    Optional<Map.Entry<ExtensionModel, DslSyntaxResolver>> entry =
        resolvers.entrySet().stream()
            .filter(e -> e.getKey().getXmlDslModel().getNamespaceUri().equals(identifier.getNamespace()))
            .findFirst();

    if (!entry.isPresent()) {
      return null;
    }

    currentExtension = entry.get().getKey();
    dsl = entry.get().getValue();


    Reference<DslElementModel> elementModel = new Reference<>();
    new ExtensionWalker() {

      @Override
      protected void onConfiguration(ConfigurationModel model) {
        final DslElementSyntax elementDsl = dsl.resolve(model);
        getIdentifier(elementDsl).ifPresent(elementId -> {
          if (elementId.equals(identifier)) {
            DslElementModel.Builder<ConfigurationModel> element = createElementModel(model, elementDsl, configuration);
            addConnectionProvider(model, dsl, element, configuration);
            elementModel.set(element.build());
            stop();
          }
        });

      }

      @Override
      protected void onOperation(HasOperationModels owner, OperationModel model) {
        final DslElementSyntax elementDsl = dsl.resolve(model);
        getIdentifier(elementDsl).ifPresent(elementId -> {
          if (elementId.equals(identifier)) {
            elementModel.set(createElementModel(model, elementDsl, configuration).build());
            stop();
          }
        });
      }

      @Override
      protected void onSource(HasSourceModels owner, SourceModel model) {
        final DslElementSyntax elementDsl = dsl.resolve(model);
        getIdentifier(elementDsl).ifPresent(elementId -> {
          if (elementId.equals(identifier)) {
            elementModel.set(createElementModel(model, elementDsl, configuration).build());
            stop();
          }
        });
      }

    }.walk(currentExtension);

    if (elementModel.get() == null) {
      resolveBasedOnTypes(configuration)
          .ifPresent(elementModel::set);
    }

    return elementModel.get();
  }

  private Optional<DslElementModel<ObjectType>> resolveBasedOnTypes(ComponentConfiguration configuration) {
    return currentExtension.getTypes().stream()
        .map(type -> {
          Optional<DslElementSyntax> typeDsl = dsl.resolve(type);
          if (typeDsl.isPresent()) {
            Optional<ComponentIdentifier> elementIdentifier = getIdentifier(typeDsl.get());
            if (elementIdentifier.isPresent() && elementIdentifier.get().equals(configuration.getIdentifier())) {
              return DslElementModel.<ObjectType>builder()
                  .withModel(type)
                  .withDsl(typeDsl.get())
                  .withConfig(configuration)
                  .build();
            }
          }
          return null;
        }).filter(Objects::nonNull)
        .findFirst();
  }

  private DslElementModel.Builder<ConfigurationModel> addConnectionProvider(ConfigurationModel model,
                                                                            DslSyntaxResolver dsl,
                                                                            DslElementModel.Builder<ConfigurationModel> element,
                                                                            ComponentConfiguration configuration) {

    concat(model.getConnectionProviders().stream(), currentExtension.getConnectionProviders()
        .stream())
            .map(provider -> {
              DslElementSyntax providerDsl = dsl.resolve(provider);
              ComponentIdentifier identifier = getIdentifier(providerDsl).orElse(null);
              return configuration.getNestedComponents().stream()
                  .filter(c -> c.getIdentifier().equals(identifier))
                  .findFirst()
                  .map(providerConfig -> element.containing(createElementModel(provider, providerDsl, providerConfig).build()))
                  .orElse(null);
            })
            .filter(Objects::nonNull)
            .findFirst();

    return element;
  }

  private <T extends ParameterizedModel> DslElementModel.Builder<T> createElementModel(T model, DslElementSyntax elementDsl,
                                                                                       ComponentConfiguration configuration) {
    DslElementModel.Builder<T> builder = DslElementModel.builder();
    builder.withModel(model)
        .withDsl(elementDsl)
        .withConfig(configuration);

    populateParameterizedElements(model, elementDsl, builder, configuration);
    return builder;
  }

  private void populateParameterizedElements(ParameterizedModel model, DslElementSyntax elementDsl,
                                             DslElementModel.Builder builder, ComponentConfiguration configuration) {

    Map<ComponentIdentifier, ComponentConfiguration> innerComponents = configuration.getNestedComponents().stream()
        .collect(toMap(ComponentConfiguration::getIdentifier, e -> e));

    Map<String, String> parameters = configuration.getParameters();

    List<ParameterModel> inlineGroupedParameters = model.getParameterGroupModels().stream()
        .filter(ParameterGroupModel::isShowInDsl)
        .peek(group -> addInlineGroup(elementDsl, innerComponents, parameters, group))
        .flatMap(g -> g.getParameterModels().stream())
        .collect(toList());

    model.getAllParameterModels().stream()
        .filter(p -> !inlineGroupedParameters.contains(p))
        .forEach(p -> addElementParameter(innerComponents, parameters, elementDsl, builder, p));
  }

  private void addInlineGroup(DslElementSyntax elementDsl, Map<ComponentIdentifier, ComponentConfiguration> innerComponents,
                              Map<String, String> parameters, ParameterGroupModel group) {
    elementDsl.getChild(group.getName())
        .ifPresent(groupDsl -> {
          ComponentConfiguration groupComponent = getIdentifier(groupDsl).map(innerComponents::get).orElse(null);

          if (groupComponent != null) {
            DslElementModel.Builder<ParameterGroupModel> groupElementBuilder = DslElementModel.<ParameterGroupModel>builder()
                .withModel(group)
                .withDsl(groupDsl)
                .withConfig(groupComponent);

            group.getParameterModels()
                .forEach(p -> addElementParameter(innerComponents, parameters, groupDsl, groupElementBuilder, p));
          }
        });
  }

  private void addElementParameter(Map<ComponentIdentifier, ComponentConfiguration> innerComponents,
                                   Map<String, String> parameters,
                                   DslElementSyntax groupDsl, DslElementModel.Builder<ParameterGroupModel> groupElementBuilder,
                                   ParameterModel p) {

    groupDsl.getContainedElement(p.getName())
        .ifPresent(pDsl -> {
          ComponentConfiguration paramComponent = getIdentifier(pDsl).map(innerComponents::get).orElse(null);

          if (!pDsl.isWrapped()) {
            String paramValue = pDsl.supportsAttributeDeclaration() ? parameters.get(pDsl.getAttributeName()) : null;
            if (paramComponent != null || paramValue != null) {
              DslElementModel.Builder<ParameterModel> paramElement =
                  DslElementModel.<ParameterModel>builder().withModel(p).withDsl(pDsl);

              if (paramComponent != null) {
                paramElement.withConfig(paramComponent);

                if (paramComponent.getNestedComponents().size() > 0) {
                  paramComponent.getNestedComponents().forEach(c -> this.create(c).ifPresent(paramElement::containing));
                }
              }

              groupElementBuilder.containing(paramElement.build());
            }
          } else {
            resolveWrappedElement(groupElementBuilder, p, pDsl, paramComponent);
          }
        });
  }

  private void resolveWrappedElement(DslElementModel.Builder<ParameterGroupModel> groupElementBuilder, ParameterModel p,
                                     DslElementSyntax pDsl, ComponentConfiguration paramComponent) {
    if (paramComponent != null) {
      DslElementModel.Builder<ParameterModel> paramElement =
          DslElementModel.<ParameterModel>builder().withModel(p).withDsl(pDsl).withConfig(paramComponent);

      if (paramComponent.getNestedComponents().size() > 0) {
        ComponentConfiguration wrappedComponent = paramComponent.getNestedComponents().get(0);
        this.create(wrappedComponent).ifPresent(paramElement::containing);
      }

      groupElementBuilder.containing(paramElement.build());
    }
  }

  private Optional<ComponentIdentifier> getIdentifier(DslElementSyntax dsl) {
    if (dsl.supportsTopLevelDeclaration() || dsl.supportsChildDeclaration()) {
      return Optional.of(ComponentIdentifier.builder()
          .withName(dsl.getElementName())
          .withNamespace(dsl.getNamespaceUri())
          .build());
    }

    return empty();
  }

}
