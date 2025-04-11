/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.dsl.model;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isMap;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.getDefaultValue;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.isContent;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.isRequired;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.isText;
import static org.mule.runtime.config.internal.dsl.utils.DslConstants.KEY_ATTRIBUTE_NAME;
import static org.mule.runtime.config.internal.dsl.utils.DslConstants.VALUE_ATTRIBUTE_NAME;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.ast.api.MetadataTypeAdapter;
import org.mule.runtime.ast.api.builder.ComponentAstBuilder;
import org.mule.runtime.config.api.dsl.model.DslElementModelFactory;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.runtime.metadata.api.dsl.DslElementModel;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.slf4j.Logger;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * Implementation of {@link DslElementModelFactory} that creates a {@link DslElementModel} based on its {@link ComponentAst}
 * representation.
 *
 * @since 4.4
 */
class ComponentAstBasedElementModelFactory {

  private static final Logger LOGGER = getLogger(ComponentAstBasedElementModelFactory.class);

  private final ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();

  public <T> Optional<DslElementModel<T>> create(ComponentAst configuration) {
    return configuration.getModel(ParameterizedModel.class)
        .flatMap(model -> of((DslElementModel<T>) createElementModel(model,
                                                                     configuration.getGenerationInformation().getSyntax().get(),
                                                                     configuration)
            .build()));
  }

  private Optional<DslElementModel.Builder<ObjectType>> resolveBasedOnType(ObjectType type,
                                                                           ComponentAst configuration) {
    return configuration.getGenerationInformation().getSyntax()
        .flatMap(typeDsl -> {
          Optional<ComponentIdentifier> elementIdentifier = getIdentifier(typeDsl);

          if (elementIdentifier.isPresent()
              && configuration.getModel(MetadataTypeAdapter.class).map(mtma -> mtma.isWrapperFor(type)).orElse(false)) {
            DslElementModel.Builder<ObjectType> typeBuilder = DslElementModel.<ObjectType>builder()
                .withModel(type)
                .withDsl(typeDsl)
                .withConfig(configuration);

            enrichElementModel(configuration.getModel(ParameterizedModel.class).get(), typeDsl, configuration, typeBuilder);

            return of(typeBuilder);
          } else {
            return empty();
          }
        });
  }

  private Multimap<ComponentIdentifier, ComponentAst> getNestedComponents(ComponentAst configuration) {
    Multimap<ComponentIdentifier, ComponentAst> result = ArrayListMultimap.create();
    configuration.directChildrenStream()
        .forEach(componentConfiguration -> result.put(componentConfiguration.getIdentifier(), componentConfiguration));
    return result;
  }

  private MetadataTypeVisitor getComponentChildVisitor(final DslElementModel.Builder typeBuilder,
                                                       final ComponentAst configuration,
                                                       final MetadataType model, final String groupName, final String name,
                                                       final DslElementSyntax modelDsl, final Optional<String> defaultValue) {

    return new MetadataTypeVisitor() {

      @Override
      protected void defaultVisit(MetadataType metadataType) {
        DslElementModel.Builder<MetadataType> elementBuilder = DslElementModel.<MetadataType>builder()
            .withModel(model)
            .withDsl(modelDsl);

        Optional<ComponentIdentifier> identifier = getIdentifier(modelDsl);

        final ComponentParameterAst param = configuration.getParameter(groupName, name);
        if (param != null) {
          String value = param.getRawValue();
          LOGGER.trace("getComponentChildVisitor#defaultVisit: '{}': '{}'", identifier, value);
          if (!isBlank(value)) {
            typeBuilder.containing(elementBuilder.withValue(value).build());
          }
        }
      }

      @Override
      public void visitArrayType(ArrayType arrayType) {
        Optional<ComponentIdentifier> identifier = getIdentifier(modelDsl);
        if (!identifier.isPresent()) {
          LOGGER.trace("getComponentChildVisitor#visitArrayType: noIdentifier");
          visitNoIdentifier(typeBuilder, model, modelDsl, defaultValue);
          return;
        }

        LOGGER.trace("getComponentChildVisitor#visitArrayType: '{}'", identifier.get());
        ComponentAst fieldComponent = getSingleComponentConfiguration(getNestedComponents(configuration), identifier);
        if (fieldComponent != null) {
          DslElementModel.Builder<Object> list = DslElementModel.builder()
              .withModel(model)
              .withDsl(fieldComponent.getGenerationInformation().getSyntax().get())
              .withConfig(fieldComponent);

          modelDsl.getGeneric(arrayType.getType())
              .ifPresent(itemdsl -> {
                ComponentIdentifier itemIdentifier = getIdentifier(itemdsl).get();

                fieldComponent.directChildrenStream()
                    .filter(c -> c.getIdentifier().equals(itemIdentifier))
                    .forEach(c -> getComponentChildVisitor(list, c, arrayType.getType(), DEFAULT_GROUP_NAME, VALUE_ATTRIBUTE_NAME,
                                                           itemdsl,
                                                           defaultValue));

              });

          typeBuilder.containing(list.build());
        }
      }

      @Override
      public void visitObject(ObjectType objectType) {
        Optional<ComponentIdentifier> identifier = getIdentifier(modelDsl);
        if (!identifier.isPresent()) {
          LOGGER.trace("getComponentChildVisitor#visitObject: noIdentifier");
          visitNoIdentifier(typeBuilder, model, modelDsl, defaultValue);
        } else {
          LOGGER.trace("getComponentChildVisitor#visitObject: '{}'", identifier.get());
        }

        final ComponentAst fieldComponent =
            (ComponentAst) configuration.getParameter(DEFAULT_GROUP_NAME, "value").getValue().getRight();

        if (isMap(objectType)) {
          LOGGER.trace("getComponentChildVisitor#visitObject: '{}' -> isMap", identifier.orElse(null));
          typeBuilder.containing(createMapElement(objectType, modelDsl,
                                                  fieldComponent));
          return;
        }

        final ComponentParameterAst param = configuration.getParameter(groupName, name);
        String value = param != null ? param.getRawValue() : null;
        if (!isBlank(value)) {
          typeBuilder.containing(DslElementModel.builder()
              .withModel(model)
              .withDsl(modelDsl)
              .withValue(value)
              .build());
        } else {
          resolveBasedOnType(objectType, fieldComponent)
              .ifPresent(elementBuilder -> typeBuilder.containing(elementBuilder
                  .withDsl(modelDsl)
                  .build()));
        }
      }

      private void visitNoIdentifier(final DslElementModel.Builder typeBuilder, final MetadataType model,
                                     final DslElementSyntax modelDsl, final Optional<String> defaultValue) {
        defaultValue.ifPresent(s -> typeBuilder.containing(DslElementModel.builder()
            .withModel(model)
            .withDsl(modelDsl)
            .withValue(defaultValue.get())
            .isExplicitInDsl(false)
            .build()));
      }
    };
  }

  private DslElementModel createMapElement(ObjectType objectType, DslElementSyntax modelDsl,
                                           ComponentAst configuration) {
    DslElementModel.Builder<Object> mapBuilder = DslElementModel.builder()
        .withModel(objectType)
        .withDsl(configuration.getGenerationInformation().getSyntax().get())
        .withConfig(configuration);

    if (configuration != null && configuration.directChildrenStream().count() > 0) {
      populateMapEntries(objectType, modelDsl, mapBuilder, configuration);
    }

    return mapBuilder.build();
  }

  private void populateMapEntries(ObjectType objectType, DslElementSyntax modelDsl, DslElementModel.Builder mapBuilder,
                                  ComponentAst mapConfig) {
    populateMapEntries(objectType, modelDsl, mapBuilder, mapConfig.directChildrenStream().collect(toList()));
  }

  private void populateMapEntries(ObjectType objectType, DslElementSyntax modelDsl, DslElementModel.Builder mapBuilder,
                                  Collection<ComponentAst> mapEntriesConfigs) {
    mapEntriesConfigs.forEach(entryConfig -> {
      MetadataType entryType = objectType.getOpenRestriction().get();
      modelDsl.getGeneric(entryType).ifPresent(entryDsl -> {
        DslElementModel.Builder<Object> entry = DslElementModel.builder()
            .withModel(entryType)
            .withConfig(entryConfig)
            .withDsl(entryConfig.getGenerationInformation().getSyntax().get());

        entry.containing(DslElementModel.builder()
            .withModel(typeLoader.load(String.class))
            .withValue(entryConfig.getParameter(DEFAULT_GROUP_NAME, KEY_ATTRIBUTE_NAME).getRawValue())
            .withDsl(entryDsl.getAttribute(KEY_ATTRIBUTE_NAME).get())
            .build());

        String value = entryConfig.getParameter(DEFAULT_GROUP_NAME, VALUE_ATTRIBUTE_NAME).getRawValue();
        if (isBlank(value)) {
          entryType.accept(getComponentChildVisitor(entry, entryConfig, entryType,
                                                    DEFAULT_GROUP_NAME, VALUE_ATTRIBUTE_NAME,
                                                    entryDsl.getAttribute(VALUE_ATTRIBUTE_NAME).get(),
                                                    empty()));
        } else {
          entry.containing(DslElementModel.builder()
              .withModel(typeLoader.load(String.class))
              .withValue(value)
              .withDsl(entryDsl.getAttribute(VALUE_ATTRIBUTE_NAME).get())
              .build());
        }

        mapBuilder.containing(entry.build());
      });
    });
  }

  private <T extends ParameterizedModel> DslElementModel.Builder<T> createElementModel(T model, DslElementSyntax elementDsl,
                                                                                       ComponentAst configuration) {
    DslElementModel.Builder<T> builder = DslElementModel.builder();
    builder.withModel(model)
        .withDsl(configuration.getGenerationInformation().getSyntax().get())
        .withConfig(configuration);

    enrichElementModel(model, elementDsl, configuration, builder);

    return builder;
  }

  private <T extends ParameterizedModel> void enrichElementModel(T model, DslElementSyntax elementDsl,
                                                                 ComponentAst configuration,
                                                                 DslElementModel.Builder builder) {
    // Have to keep the order of the elements consistent so the generated metadata keys are backwards compatible
    populateConnectionProviderElements(builder, configuration);
    populateParameterizedElements(model, elementDsl, builder, configuration,
                                  paramGroupModel -> paramModel -> configuration.getParameter(paramGroupModel.getName(),
                                                                                              paramModel.getName()));
    populateComposableElements(builder, configuration);

    if (model instanceof SourceModel) {
      ((SourceModel) model).getSuccessCallback()
          .ifPresent(cb -> populateParameterizedElements(cb, elementDsl, builder, configuration,
                                                         paramGroupModel -> paramModel -> configuration
                                                             .getParameter(paramGroupModel.getName(), paramModel.getName())));

      ((SourceModel) model).getErrorCallback()
          .ifPresent(cb -> populateParameterizedElements(cb, elementDsl, builder, configuration,
                                                         paramGroupModel -> paramModel -> configuration
                                                             .getParameter(paramGroupModel.getName(), paramModel.getName())));
    }
  }

  private void populateParameterizedElements(ParameterizedModel model, DslElementSyntax elementDsl,
                                             DslElementModel.Builder builder, ComponentAst configuration,
                                             Function<ParameterGroupModel, Function<ParameterModel, ComponentParameterAst>> paramFetcher) {
    Map<String, String> parameters = configuration.getParameters()
        .stream()
        .filter(p -> p.getResolvedRawValue() != null)
        .collect(toMap(p -> p.getModel().getName(), p -> p.getResolvedRawValue(), (u, v) -> u));

    model.getParameterGroupModels().stream()
        .filter(ParameterGroupModel::isShowInDsl)
        .forEach(group -> addInlineGroup(configuration, parameters, builder, group,
                                         paramFetcher.apply(group)));


    model.getParameterGroupModels().stream()
        .filter(g -> !g.isShowInDsl())
        .forEach(g -> {
          g.getParameterModels().forEach(p -> {
            addElementParameter(configuration, parameters, elementDsl, builder, g, p,
                                paramModel -> configuration.getParameter(g.getName(), paramModel.getName()));
          });
        });
  }

  private void populateConnectionProviderElements(DslElementModel.Builder builder, ComponentAst configuration) {
    configuration.directChildrenStream()
        .filter(c -> c.getModel(ConnectionProviderModel.class).isPresent())
        .forEach(nestedComponentConfig -> create(nestedComponentConfig).ifPresent(builder::containing));
  }

  private void populateComposableElements(DslElementModel.Builder builder, ComponentAst configuration) {
    configuration.directChildrenStream()
        .filter(c -> !c.getModel(ConnectionProviderModel.class).isPresent())
        .forEach(nestedComponentConfig -> create(nestedComponentConfig).ifPresent(builder::containing));
  }

  private void addInlineGroup(ComponentAst configuration, Map<String, String> parameters,
                              DslElementModel.Builder parent, ParameterGroupModel group,
                              Function<ParameterModel, ComponentParameterAst> paramFetcher) {
    final DslElementSyntax groupSyntax =
        configuration.getGenerationInformation().getSyntax().flatMap(gs -> gs.getChild(group.getName())).get();
    Optional<ComponentIdentifier> identifier = getIdentifier(groupSyntax);
    if (!identifier.isPresent()) {
      return;
    }
    DslElementModel.Builder<ParameterGroupModel> groupElementBuilder = DslElementModel.<ParameterGroupModel>builder()
        .withModel(group)
        .withDsl(groupSyntax);

    if (configuration.getParameters().stream()
        .filter(param -> !param.isDefaultValue())
        .anyMatch(param -> param.getGroupModel().equals(group))) {
      groupElementBuilder.withGroupConfig(configuration, group);

      group.getParameterModels()
          .forEach(p -> addElementParameter(configuration, parameters, groupSyntax, groupElementBuilder,
                                            group, p, paramFetcher));

      parent.containing(groupElementBuilder.build());

    } else if (shouldBuildDefaultGroup(group)) {
      buildDefaultInlineGroupElement(parent, groupElementBuilder.isExplicitInDsl(false), group, groupSyntax,
                                     identifier.get());
    }
  }

  private ComponentAst getSingleComponentConfiguration(Multimap<ComponentIdentifier, ComponentAst> innerComponents,
                                                       Optional<ComponentIdentifier> identifier) {
    return identifier.filter(innerComponents::containsKey)
        .map(innerComponents::get)
        .map(collection -> collection.iterator().next())
        .orElse(null);
  }

  private void buildDefaultInlineGroupElement(DslElementModel.Builder parent,
                                              DslElementModel.Builder<ParameterGroupModel> groupElementBuilder,
                                              ParameterGroupModel group,
                                              DslElementSyntax groupDsl,
                                              ComponentIdentifier identifier) {
    final ComponentAstBuilder groupCompAstBuilder = ComponentAstBuilder.builder();

    groupCompAstBuilder.withIdentifier(identifier);

    group.getParameterModels()
        .forEach(paramModel -> groupDsl.getContainedElement(paramModel.getName())
            .ifPresent(paramDsl -> getDefaultValue(paramModel)
                .ifPresent(defaultValue -> {
                  DslElementModel.Builder<ParameterModel> paramElementBuilder = DslElementModel.<ParameterModel>builder()
                      .withModel(paramModel)
                      .withDsl(paramDsl)
                      .isExplicitInDsl(false)
                      .withValue(defaultValue);

                  if (isContent(paramModel) || isText(paramModel)) {
                    getIdentifier(paramDsl)
                        .ifPresent(tagId -> groupCompAstBuilder.addChildComponent()
                            .withIdentifier(tagId)
                            .withBodyParameter(defaultValue)
                            .build());
                  } else {
                    groupCompAstBuilder.withRawParameter(defaultValue, paramDsl.getAttributeName());
                  }

                  groupElementBuilder.containing(paramElementBuilder.build());
                })));

    groupElementBuilder.withConfig(groupCompAstBuilder.build());
    parent.containing(groupElementBuilder.build());
  }

  private boolean shouldBuildDefaultGroup(ParameterGroupModel group) {
    return !isRequired(group) && group.getParameterModels().stream().anyMatch(p -> getDefaultValue(p).isPresent());
  }

  private void addElementParameter(ComponentAst configuration, Map<String, String> parameters,
                                   DslElementSyntax groupDsl, DslElementModel.Builder<ParameterGroupModel> groupElementBuilder,
                                   ParameterGroupModel groupModel, ParameterModel paramModel,
                                   Function<ParameterModel, ComponentParameterAst> paramFetcher) {
    final DslElementSyntax paramSyntax = groupDsl.getContainedElement(paramModel.getName())
        .orElseGet(() -> configuration.getGenerationInformation().getSyntax().get().getContainedElement(paramModel.getName())
            .orElse(null));
    if (paramSyntax == null) {
      return;
    }

    final ComponentParameterAst parameter = configuration.getParameter(groupModel.getName(), paramModel.getName());
    final Object paramValue = parameter != null ? parameter.getValue().getRight() : null;

    Object paramComponent;
    if (parameter != null
        // handle nested parameters
        && (paramValue instanceof ComponentAst || paramValue instanceof Collection)) {

      if (paramValue instanceof Collection && ((Collection) paramValue).isEmpty()) {
        // assume an empty collection parameter as the parameter is not present
        paramComponent = null;
      } else {
        paramComponent = paramValue;
      }

      if (paramSyntax.isWrapped() && paramComponent instanceof ComponentAst) {
        resolveWrappedElement(groupElementBuilder, paramModel, (ComponentAst) paramComponent, paramSyntax);
        return;
      }
    } else {
      paramComponent = null;
    }

    String value = paramSyntax.supportsAttributeDeclaration()
        ? parameters.get(paramSyntax.getAttributeName())
        : (parameter != null
            ? parameter.getRawValue()
            : null);

    Optional<String> defaultValue = getDefaultValue(paramModel);
    if (paramComponent != null || !isBlank(value) || defaultValue.isPresent()) {
      DslElementModel.Builder<ParameterModel> paramElementBuilder = DslElementModel.<ParameterModel>builder()
          .withModel(paramModel)
          .withDsl(paramSyntax);

      if (paramComponent != null && !isContent(paramModel) && !isText(paramModel)) {
        paramModel.getType().accept(new MetadataTypeVisitor() {

          @Override
          protected void defaultVisit(MetadataType metadataType) {
            if (paramComponent instanceof ComponentAst) {
              final ComponentAst paramComponentValue = (ComponentAst) paramComponent;
              paramElementBuilder.withConfig(paramComponentValue);
              enrichElementModel(paramComponentValue.getModel(ParameterizedModel.class).get(),
                                 groupDsl, paramComponentValue, paramElementBuilder);
            }
          }

          @Override
          public void visitArrayType(ArrayType arrayType) {
            MetadataType itemType = arrayType.getType();
            paramSyntax.getGeneric(itemType)
                .ifPresent(itemdsl -> ((Collection<ComponentAst>) paramValue)
                    .stream()
                    .filter(c -> c.getModel(MetadataTypeAdapter.class).map(mtma -> mtma.isWrapperFor(itemType))
                        .orElse(false))
                    .forEach(c -> {
                      final DslElementModel.Builder<Object> arrayModelBuilder = DslElementModel.builder()
                          .withModel(itemType)
                          .withDsl(c.getGenerationInformation().getSyntax().get())
                          .withConfig(c);
                      enrichElementModel(c.getModel(ParameterizedModel.class).get(), itemdsl, c,
                                         arrayModelBuilder);
                      paramElementBuilder.containing(arrayModelBuilder.build());
                    }));
          }

          @Override
          public void visitObject(ObjectType objectType) {
            if (isMap(objectType)) {
              populateMapEntries(objectType, paramSyntax, paramElementBuilder,
                                 ((Collection<ComponentAst>) paramValue));
              return;
            }

            defaultVisit(objectType);
          }
        });
      } else if (isBlank(value)) {
        final Optional<String> body = paramComponent != null
            ? of(paramFetcher.apply(paramModel).getRawValue())
            : empty();

        paramElementBuilder.withValue(body
            .map(String::trim)
            .orElseGet(() -> {
              paramElementBuilder.isExplicitInDsl(false);
              return defaultValue.get();
            }));
      } else {
        paramElementBuilder.withValue(value);
      }

      groupElementBuilder.containing(paramElementBuilder.build());
    }
  }

  protected boolean paramIsEmptyCollection(final ComponentParameterAst parameter) {
    return parameter.getValue().getRight() instanceof Collection && ((Collection) parameter.getValue().getRight()).isEmpty();
  }

  private void resolveWrappedElement(DslElementModel.Builder<ParameterGroupModel> groupElementBuilder, ParameterModel p,
                                     ComponentAst paramComponent, DslElementSyntax paramSyntax) {
    if (paramComponent != null) {
      DslElementModel.Builder<ParameterModel> paramElement = DslElementModel.<ParameterModel>builder()
          .withModel(p)
          .withDsl(paramSyntax);

      create(paramComponent)
          .ifPresent(paramElement::containing);

      groupElementBuilder.containing(paramElement.build());
    }
  }

  private Optional<ComponentIdentifier> getIdentifier(DslElementSyntax dsl) {
    if (isNotBlank(dsl.getElementName()) && isNotBlank(dsl.getPrefix())) {
      return Optional.of(builder()
          .name(dsl.getElementName())
          .namespace(dsl.getPrefix())
          .build());
    }

    return empty();
  }

}
