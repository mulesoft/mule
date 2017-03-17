/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.model.internal;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;
import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.internal.dsl.DslConstants.CONFIG_ATTRIBUTE_NAME;
import static org.mule.runtime.internal.dsl.DslConstants.KEY_ATTRIBUTE_NAME;
import static org.mule.runtime.internal.dsl.DslConstants.NAME_ATTRIBUTE_NAME;
import static org.mule.runtime.internal.dsl.DslConstants.VALUE_ATTRIBUTE_NAME;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getAlias;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isMap;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isParameterGroup;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.getDefaultValue;
import static org.mule.runtime.extension.api.util.ExtensionModelUtils.isInfrastructure;
import static org.mule.runtime.extension.internal.dsl.syntax.DslSyntaxUtils.getId;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.app.declaration.ComponentElementDeclaration;
import org.mule.runtime.api.app.declaration.ConfigurationElementDeclaration;
import org.mule.runtime.api.app.declaration.ConnectionElementDeclaration;
import org.mule.runtime.api.app.declaration.ElementDeclaration;
import org.mule.runtime.api.app.declaration.OperationElementDeclaration;
import org.mule.runtime.api.app.declaration.ParameterElementDeclaration;
import org.mule.runtime.api.app.declaration.ParameterValue;
import org.mule.runtime.api.app.declaration.ParameterValueVisitor;
import org.mule.runtime.api.app.declaration.ParameterizedElementDeclaration;
import org.mule.runtime.api.app.declaration.SourceElementDeclaration;
import org.mule.runtime.api.app.declaration.TopLevelParameterDeclaration;
import org.mule.runtime.api.app.declaration.fluent.ParameterListValue;
import org.mule.runtime.api.app.declaration.fluent.ParameterObjectValue;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
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
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Implementation of {@link DslElementModelFactory} that creates a {@link DslElementModel} based on its {@link ElementDeclaration}
 * representation.
 *
 * @since 4.0
 */
class DeclarationBasedElementModelFactory {

  private final DslResolvingContext context;
  private final InfrastructureElementModelDelegate infrastructureDelegate;
  private final ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();
  private Map<ExtensionModel, DslSyntaxResolver> resolvers;
  private ExtensionModel currentExtension;
  private DslSyntaxResolver dsl;

  DeclarationBasedElementModelFactory(DslResolvingContext context, Map<ExtensionModel, DslSyntaxResolver> resolvers) {
    this.context = context;
    this.resolvers = resolvers;
    this.infrastructureDelegate = new InfrastructureElementModelDelegate();
  }

  public <T> Optional<DslElementModel<T>> create(ElementDeclaration declaration) {

    setupCurrentExtensionContext(declaration.getDeclaringExtension());
    final Function<NamedObject, Boolean> equalsName = (named) -> named.getName().equals(declaration.getName());

    if (declaration instanceof TopLevelParameterDeclaration) {
      return createFromType((TopLevelParameterDeclaration) declaration);
    }

    Reference<DslElementModel> elementModel = new Reference<>();
    new ExtensionWalker() {

      @Override
      protected void onConfiguration(ConfigurationModel model) {
        if (equalsName.apply(model)) {
          checkArgument(declaration instanceof ConfigurationElementDeclaration,
                        format("Found a Configuration with the given name, but expected a '%s'",
                               declaration.getClass().getName()));
          elementModel.set(createConfigurationElement(model, (ConfigurationElementDeclaration) declaration));
          stop();
        }
      }

      @Override
      protected void onOperation(HasOperationModels owner, OperationModel model) {
        if (equalsName.apply(model)) {
          checkArgument(declaration instanceof OperationElementDeclaration,
                        format("Found an Operation with the given name, but expected a '%s'",
                               declaration.getClass().getName()));
          elementModel.set(createComponentElement(model, (OperationElementDeclaration) declaration));
          stop();
        }
      }

      @Override
      protected void onSource(HasSourceModels owner, SourceModel model) {
        if (equalsName.apply(model)) {
          checkArgument(declaration instanceof SourceElementDeclaration,
                        format("Found a Source with the given name, but expected a '%s'",
                               declaration.getClass().getName()));
          elementModel.set(createComponentElement(model, (SourceElementDeclaration) declaration));
          stop();
        }
      }

    }.walk(currentExtension);

    return Optional.ofNullable(elementModel.get());
  }

  private <T> Optional<DslElementModel<T>> createFromType(TopLevelParameterDeclaration declaration) {
    Optional<ObjectType> type = context.getTypeCatalog().getType(declaration.getValue()
        .getTypeId());
    if (!type.isPresent()) {
      return Optional.empty();
    }
    return Optional.of(createTopLevelElement(type.get(), declaration));
  }

  private DslElementModel<ConfigurationModel> createConfigurationElement(ConfigurationModel model,
                                                                         ConfigurationElementDeclaration configDeclaration) {
    DslElementSyntax configDsl = dsl.resolve(model);

    ComponentConfiguration.Builder configuration = ComponentConfiguration.builder()
        .withIdentifier(asIdentifier(configDsl))
        .withParameter(NAME_ATTRIBUTE_NAME, configDeclaration.getRefName());

    DslElementModel.Builder<ConfigurationModel> element =
        createParameterizedElementModel(model, configDsl, configDeclaration, configuration);

    if (configDeclaration.getConnection() != null) {
      addConnectionProvider(configDeclaration.getConnection(), model, configuration, element);
    }

    return element.withConfig(configuration.build()).build();
  }

  private DslElementModel<? extends ComponentModel> createComponentElement(ComponentModel model,
                                                                           ComponentElementDeclaration componentDeclaration) {
    DslElementSyntax configDsl = dsl.resolve(model);
    ComponentConfiguration.Builder configuration = ComponentConfiguration.builder()
        .withIdentifier(asIdentifier(configDsl));

    if (componentDeclaration.getConfigRef() != null) {
      configuration.withParameter(CONFIG_ATTRIBUTE_NAME, componentDeclaration.getConfigRef());
    }

    DslElementModel.Builder<? extends ComponentModel> element =
        createParameterizedElementModel(model, configDsl, componentDeclaration, configuration);
    return element.withConfig(configuration.build()).build();
  }

  private DslElementModel createTopLevelElement(ObjectType model, TopLevelParameterDeclaration declaration) {

    DslElementSyntax objectDsl = dsl.resolve(model).orElseThrow(() -> new IllegalArgumentException());
    DslElementModel.Builder<MetadataType> parentElement = DslElementModel.<MetadataType>builder()
        .withModel(model)
        .withDsl(objectDsl);

    ComponentConfiguration.Builder configuration = ComponentConfiguration.builder()
        .withIdentifier(asIdentifier(objectDsl))
        .withParameter(NAME_ATTRIBUTE_NAME, declaration.getRefName());

    populateObjectElementFields(model, declaration.getValue(), objectDsl, configuration, parentElement);

    return parentElement.withConfig(configuration.build()).build();
  }

  private void setupCurrentExtensionContext(String extension) {
    this.currentExtension = context.getExtension(extension).orElseThrow(() -> new IllegalArgumentException());
    this.dsl = resolvers.get(currentExtension);
  }

  private void addConnectionProvider(ConnectionElementDeclaration connection,
                                     ConfigurationModel model,
                                     ComponentConfiguration.Builder configuration,
                                     DslElementModel.Builder<ConfigurationModel> configElement) {

    concat(model.getConnectionProviders().stream(), currentExtension.getConnectionProviders()
        .stream())
            .filter(c -> c.getName().equals(connection.getName()))
            .findFirst()
            .ifPresent(provider -> {
              DslElementSyntax providerDsl = dsl.resolve(provider);

              ComponentConfiguration.Builder builder = ComponentConfiguration.builder()
                  .withIdentifier(asIdentifier(providerDsl));

              DslElementModel.Builder<ConnectionProviderModel> element =
                  createParameterizedElementModel(provider, providerDsl, connection, builder);

              ComponentConfiguration providerConfig = builder.build();

              configuration.withNestedComponent(providerConfig);
              configElement.containing(element.withConfig(providerConfig).build());
            });
  }


  private <T extends ParameterizedModel> DslElementModel.Builder<T> createParameterizedElementModel(T model,
                                                                                                    DslElementSyntax elementDsl,
                                                                                                    ParameterizedElementDeclaration declaration,
                                                                                                    ComponentConfiguration.Builder parentConfig) {
    DslElementModel.Builder<T> parentElement = DslElementModel.<T>builder()
        .withModel(model)
        .withDsl(elementDsl);

    List<ParameterModel> inlineGroupedParameters = model.getParameterGroupModels().stream()
        .filter(ParameterGroupModel::isShowInDsl)
        .peek(group -> addInlineGroupElement(group, elementDsl, parentConfig, parentElement, declaration))
        .flatMap(g -> g.getParameterModels().stream())
        .collect(toList());

    List<ParameterModel> nonGroupedParameters = model.getAllParameterModels().stream()
        .filter(p -> !inlineGroupedParameters.contains(p))
        .collect(toList());

    addAllDeclaredParameters(nonGroupedParameters, declaration.getParameters(), elementDsl, parentConfig, parentElement);

    return parentElement;
  }

  private void addAllDeclaredParameters(List<ParameterModel> parameterModels,
                                        List<ParameterElementDeclaration> parameterDeclarations,
                                        DslElementSyntax parentDsl,
                                        ComponentConfiguration.Builder parentConfig,
                                        DslElementModel.Builder parentElement) {

    List<String> configuredParameters = new LinkedList<>();
    parameterModels
        .forEach(paramModel -> parentDsl.getContainedElement(paramModel.getName())
            .ifPresent(paramDsl -> {
              Optional<ParameterElementDeclaration> declared = parameterDeclarations.stream()
                  .filter(d -> d.getName().equals(paramModel.getName()))
                  .findFirst();

              if (declared.isPresent()) {
                addParameter(declared.get(), paramModel, paramDsl, parentConfig, parentElement);
                configuredParameters.add(declared.get().getName());
              } else {
                getDefaultValue(paramModel)
                    .ifPresent(value -> createSimpleParameter(value, paramDsl, parentConfig, parentElement, paramModel));
              }
            }));

    if (parameterDeclarations.size() > configuredParameters.size()) {
      String missing = parameterDeclarations.stream()
          .filter(p -> !configuredParameters.contains(p.getName()))
          .map(ElementDeclaration::getName)
          .collect(joining(","));

      throw new IllegalArgumentException(
                                         format("The parameters [%s] were declared but they do not exist in the associated model",
                                                missing));
    }
  }

  private <T extends ParameterizedModel> void addInlineGroupElement(ParameterGroupModel group,
                                                                    DslElementSyntax elementDsl,
                                                                    ComponentConfiguration.Builder parentConfig,
                                                                    DslElementModel.Builder<T> parentElement,
                                                                    ParameterizedElementDeclaration declaration) {
    elementDsl.getChild(group.getName())
        .ifPresent(groupDsl -> {
          DslElementModel.Builder<ParameterGroupModel> groupElementBuilder = DslElementModel.<ParameterGroupModel>builder()
              .withModel(group)
              .withDsl(groupDsl);

          ComponentConfiguration.Builder groupBuilder = ComponentConfiguration.builder().withIdentifier(asIdentifier(groupDsl));

          addAllDeclaredParameters(group.getParameterModels(), declaration.getParameters(), groupDsl, groupBuilder,
                                   groupElementBuilder);

          ComponentConfiguration groupConfig = groupBuilder.build();
          groupElementBuilder.withConfig(groupConfig);

          parentConfig.withNestedComponent(groupConfig);
          parentElement.containing(groupElementBuilder.build());
        });
  }

  private void addParameter(ParameterElementDeclaration parameter,
                            ParameterModel parameterModel,
                            DslElementSyntax paramDsl,
                            final ComponentConfiguration.Builder parentConfig,
                            final DslElementModel.Builder parentElement) {

    if (isInfrastructure(parameterModel)) {
      infrastructureDelegate.addParameter(parameter, parameterModel, paramDsl, parentConfig, parentElement);
      return;
    }

    parameter.getValue().accept(new ParameterValueVisitor() {

      @Override
      public void visitSimpleValue(String value) {
        createSimpleParameter(value, paramDsl, parentConfig, parentElement, parameterModel);
      }

      @Override
      public void visitListValue(ParameterListValue list) {
        checkArgument(paramDsl.supportsChildDeclaration(),
                      format("Inline List values are not allowed for this parameter [%s]", parameterModel.getName()));
        checkArgument(parameterModel.getType() instanceof ArrayType,
                      format("List values can only be associated to ArrayType parameters. Parameter [%s] is of type [%s]",
                             parameterModel.getName(), getId(parameterModel.getType())));
        // the parameter is of list type, so we have nested elements
        // we'll resolve this based on the type of the parameter, since no
        // further model information is available
        createList(list, paramDsl, (ArrayType) parameterModel.getType(), parentConfig, parentElement);
      }

      @Override
      public void visitObjectValue(ParameterObjectValue objectValue) {
        checkArgument(parameterModel.getType() instanceof ObjectType,
                      format("Complex values can only be associated to ObjectType parameters. Parameter [%s] is of type [%s]",
                             parameterModel.getName(), getId(parameterModel.getType())));
        checkArgument(paramDsl.supportsChildDeclaration(),
                      format("Complex values are not allowed for this parameter [%s]", parameterModel.getName()));

        if (isMap(parameterModel.getType())) {
          createMapParameter(objectValue, paramDsl, parameterModel, (ObjectType) parameterModel.getType(),
                             parentConfig, parentElement);
        } else {
          createComplexParameter(objectValue, paramDsl, parameterModel, parentConfig, parentElement);
        }
      }
    });
  }

  private void createMapParameter(ParameterObjectValue objectValue, DslElementSyntax paramDsl, Object model, ObjectType mapType,
                                  ComponentConfiguration.Builder parentConfig, DslElementModel.Builder parentElement) {

    ComponentConfiguration.Builder mapConfig = ComponentConfiguration.builder()
        .withIdentifier(asIdentifier(paramDsl));

    DslElementModel.Builder mapElement = DslElementModel.builder()
        .withModel(model)
        .withDsl(paramDsl);

    MetadataType valueType = mapType.getOpenRestriction().get();

    paramDsl.getGeneric(valueType)
        .ifPresent(entryDsl -> objectValue.getParameters().forEach((key, value) -> {
          ComponentConfiguration.Builder entryConfigBuilder = ComponentConfiguration.builder()
              .withIdentifier(asIdentifier(entryDsl));

          DslElementModel.Builder<MetadataType> entryElement = DslElementModel.<MetadataType>builder()
              .withModel(valueType)
              .withDsl(entryDsl);


          entryDsl.getAttribute(KEY_ATTRIBUTE_NAME)
              .ifPresent(keyDsl -> {
                entryConfigBuilder.withParameter(KEY_ATTRIBUTE_NAME, key);
                entryElement.containing(DslElementModel.builder()
                    .withModel(typeLoader.load(String.class))
                    .withDsl(keyDsl)
                    .withValue(key)
                    .build());
              });

          entryDsl.getAttribute(VALUE_ATTRIBUTE_NAME)
              .ifPresent(valueDsl -> value.accept(new ParameterValueVisitor() {

                @Override
                public void visitSimpleValue(String value) {
                  entryConfigBuilder.withParameter(VALUE_ATTRIBUTE_NAME, value);
                  entryElement.containing(DslElementModel.builder()
                      .withModel(valueType)
                      .withDsl(valueDsl)
                      .withValue(value)
                      .build());
                }

                @Override
                public void visitListValue(ParameterListValue list) {
                  createList(list, valueDsl, (ArrayType) valueType, entryConfigBuilder, entryElement);
                }

                @Override
                public void visitObjectValue(ParameterObjectValue objectValue) {
                  if (isMap(valueType)) {
                    createMapParameter(objectValue, valueDsl, valueType, (ObjectType) valueType, entryConfigBuilder,
                                       entryElement);
                  } else {
                    createObject(objectValue, valueDsl, valueType, (ObjectType) valueType, entryConfigBuilder, entryElement);
                  }
                }
              }));

          ComponentConfiguration entryConfig = entryConfigBuilder.build();
          mapConfig.withNestedComponent(entryConfig);
          mapElement.containing(entryElement.withConfig(entryConfig).build());
        }));

    ComponentConfiguration result = mapConfig.build();
    parentConfig.withNestedComponent(result);

    parentElement.containing(mapElement.withConfig(result).build());
  }

  private void createComplexParameter(ParameterObjectValue objectValue, DslElementSyntax paramDsl, ParameterModel parameterModel,
                                      ComponentConfiguration.Builder parentConfig, DslElementModel.Builder parentElement) {
    if (!paramDsl.isWrapped()) {
      // the parameter is of a complex object type, so we have both nested elements
      // and attributes as values of this element.
      // we'll resolve this based on the type of the parameter, since no
      // further model information is available
      createObject(objectValue, paramDsl, parameterModel, (ObjectType) parameterModel.getType(), parentConfig, parentElement);

    } else {
      // the parameter is of an extensible object type, so we need a wrapper element
      // before defining the actual object structure
      // we'll resolve this structure based on the configured type, since no
      // further model information is available
      createWrappedObject(objectValue, parameterModel, paramDsl, parentConfig, parentElement);
    }
  }

  private void createWrappedObject(ParameterObjectValue objectValue, ParameterModel parameterModel, DslElementSyntax paramDsl,
                                   ComponentConfiguration.Builder parentConfig, DslElementModel.Builder parentElement) {
    DslElementModel.Builder<ParameterModel> wrapperElement = DslElementModel.<ParameterModel>builder()
        .withModel(parameterModel)
        .withDsl(paramDsl);

    ComponentConfiguration.Builder wrapperConfig = ComponentConfiguration.builder()
        .withIdentifier(asIdentifier(paramDsl));

    ObjectType nestedElementType;
    if (objectValue.getTypeId() == null || objectValue.getTypeId().trim().isEmpty() ||
        objectValue.getTypeId().equals(getId(parameterModel.getType()))) {

      nestedElementType = (ObjectType) parameterModel.getType();
    } else {
      nestedElementType = lookupType(objectValue);
    }

    dsl.resolve(nestedElementType)
        .ifPresent(typeDsl -> createObject(objectValue, typeDsl, nestedElementType, nestedElementType, wrapperConfig,
                                           wrapperElement));

    ComponentConfiguration result = wrapperConfig.build();

    parentConfig.withNestedComponent(result);
    parentElement.containing(wrapperElement.withConfig(result).build());
  }

  private void createSimpleParameter(String value, DslElementSyntax paramDsl, ComponentConfiguration.Builder parentConfig,
                                     DslElementModel.Builder parentElement, ParameterModel parameterModel) {
    if (paramDsl.supportsAttributeDeclaration()) {
      // attribute parameters imply no further nesting in the configs
      parentConfig.withParameter(paramDsl.getAttributeName(), value);
      parentElement.containing(DslElementModel.<ParameterModel>builder()
          .withModel(parameterModel)
          .withDsl(paramDsl)
          .withValue(value)
          .build());
    } else {
      // we are in the content case, so we have one more nesting level
      ComponentConfiguration parameterConfig = ComponentConfiguration.builder()
          .withIdentifier(asIdentifier(paramDsl))
          .withValue(value)
          .build();

      parentConfig.withNestedComponent(parameterConfig);

      parentElement.containing(DslElementModel.<ParameterModel>builder()
          .withModel(parameterModel)
          .withDsl(paramDsl)
          .withConfig(parameterConfig)
          .build());
    }
  }

  private ObjectType lookupType(ParameterObjectValue objectValue) {
    ObjectType nestedElementType;
    nestedElementType = context.getTypeCatalog().getType(objectValue.getTypeId())
        .orElseThrow(() -> new IllegalArgumentException(format("Could not find Type with ID '%s' in the current context",
                                                               objectValue.getTypeId())));
    return nestedElementType;
  }

  private void createListItemConfig(MetadataType itemValueType,
                                    ParameterValue itemValue,
                                    DslElementSyntax itemDsl,
                                    ComponentConfiguration.Builder parentConfig,
                                    DslElementModel.Builder parentElement) {

    itemValue.accept(new ParameterValueVisitor() {

      @Override
      public void visitSimpleValue(String value) {
        ComponentConfiguration item = ComponentConfiguration.builder()
            .withIdentifier(asIdentifier(itemDsl))
            .withParameter(VALUE_ATTRIBUTE_NAME, value)
            .build();

        parentConfig.withNestedComponent(item);
        parentElement.containing(DslElementModel.builder()
            .withModel(itemValueType)
            .withDsl(itemDsl)
            .withConfig(item)
            .build());
      }

      @Override
      public void visitListValue(ParameterListValue list) {
        DslElementModel.Builder<MetadataType> itemElement = DslElementModel.<MetadataType>builder()
            .withModel(itemValueType)
            .withDsl(itemDsl);

        ComponentConfiguration.Builder itemConfig = ComponentConfiguration.builder()
            .withIdentifier(asIdentifier(itemDsl));

        MetadataType genericType = ((ArrayType) itemValueType).getType();
        itemDsl.getGeneric(genericType)
            .ifPresent(genericDsl -> list.getValues()
                .forEach(value -> createListItemConfig(genericType, value, genericDsl, itemConfig, itemElement)));

        ComponentConfiguration result = itemConfig.build();

        parentConfig.withNestedComponent(result);
        parentElement.containing(itemElement.withConfig(result).build());
      }

      @Override
      public void visitObjectValue(ParameterObjectValue objectValue) {
        itemValueType.accept(new MetadataTypeVisitor() {

          @Override
          public void visitObject(ObjectType objectType) {
            if (isMap(objectType)) {
              createMapParameter(objectValue, itemDsl, itemValueType, objectType, parentConfig, parentElement);
            } else {
              createObject(objectValue, itemDsl, objectType, objectType, parentConfig, parentElement);
            }
          }
        });
      }
    });
  }

  private void addObjectField(MetadataType fieldType, ParameterValue fieldValue,
                              DslElementSyntax fieldDsl,
                              ComponentConfiguration.Builder objectConfig,
                              DslElementModel.Builder objectElement) {

    fieldValue.accept(new ParameterValueVisitor() {

      @Override
      public void visitSimpleValue(String value) {
        if (fieldDsl.supportsAttributeDeclaration()) {
          objectConfig.withParameter(fieldDsl.getAttributeName(), value);
          objectElement.containing(DslElementModel.builder()
              .withModel(fieldType)
              .withDsl(fieldDsl)
              .withValue(value)
              .build());
        } else {
          ComponentConfiguration contentConfiguration = ComponentConfiguration.builder()
              .withIdentifier(asIdentifier(fieldDsl))
              .withValue(value)
              .build();

          objectConfig.withNestedComponent(contentConfiguration);
          objectElement.containing(DslElementModel.builder()
              .withModel(fieldType)
              .withDsl(fieldDsl)
              .withConfig(contentConfiguration)
              .build());
        }
      }

      @Override
      public void visitListValue(ParameterListValue list) {
        createList(list, fieldDsl, (ArrayType) fieldType, objectConfig, objectElement);
      }

      @Override
      public void visitObjectValue(ParameterObjectValue objectValue) {

        fieldType.accept(new MetadataTypeVisitor() {

          @Override
          public void visitObject(ObjectType objectType) {
            if (isMap(objectType)) {
              createMapParameter(objectValue, fieldDsl, fieldType, objectType, objectConfig, objectElement);
            } else {
              createObject(objectValue, fieldDsl, objectType, objectType, objectConfig, objectElement);
            }
          }
        });
      }
    });
  }

  private void createList(ParameterListValue list,
                          DslElementSyntax listDsl,
                          ArrayType listType,
                          ComponentConfiguration.Builder parentConfig,
                          DslElementModel.Builder parentElement) {

    final DslElementModel.Builder<MetadataType> listElement = DslElementModel.<MetadataType>builder()
        .withModel(listType)
        .withDsl(listDsl);

    final ComponentConfiguration.Builder listConfig = ComponentConfiguration.builder()
        .withIdentifier(asIdentifier(listDsl));

    final MetadataType itemType = listType.getType();
    listDsl.getGeneric(itemType)
        .ifPresent(itemDsl -> list.getValues()
            .forEach(value -> createListItemConfig(itemType, value, itemDsl, listConfig, listElement)));

    ComponentConfiguration result = listConfig.build();

    parentConfig.withNestedComponent(result);
    parentElement.containing(listElement.withConfig(result).build());
  }

  private void createObject(ParameterObjectValue objectValue, DslElementSyntax objectDsl, Object model, ObjectType objectType,
                            ComponentConfiguration.Builder parentConfig,
                            DslElementModel.Builder parentElement) {

    ComponentConfiguration.Builder objectConfig = ComponentConfiguration.builder()
        .withIdentifier(asIdentifier(objectDsl));

    DslElementModel.Builder objectElement = DslElementModel.builder()
        .withModel(model)
        .withDsl(objectDsl);

    populateObjectElementFields(objectType, objectValue, objectDsl, objectConfig, objectElement);

    ComponentConfiguration result = objectConfig.build();
    parentConfig.withNestedComponent(result);

    parentElement.containing(objectElement.withConfig(result).build());
  }

  private void populateObjectElementFields(ObjectType objectType, ParameterObjectValue objectValue, DslElementSyntax objectDsl,
                                           ComponentConfiguration.Builder objectConfig,
                                           DslElementModel.Builder objectElement) {
    List<ObjectFieldType> fields = objectType.getFields()
        .stream()
        .flatMap(f -> isParameterGroup(f) ? ((ObjectType) f.getValue()).getFields().stream() : of(f))
        .collect(toList());

    objectValue.getParameters()
        .forEach((name, value) -> fields.stream()
            .filter(f -> getAlias(f).equals(name))
            .findFirst()
            .ifPresent(field -> objectDsl.getContainedElement(name)
                .ifPresent(nestedDsl -> addObjectField(field.getValue(), value, nestedDsl, objectConfig, objectElement))));
  }

  private ComponentIdentifier asIdentifier(DslElementSyntax fieldDsl) {
    checkArgument(fieldDsl.supportsTopLevelDeclaration() || fieldDsl.supportsChildDeclaration(),
                  format("The given component '%s' does not support element-like declaration", fieldDsl.getAttributeName()));

    return builder()
        .withName(fieldDsl.getElementName())
        .withNamespace(fieldDsl.getNamespace())
        .build();
  }

}
