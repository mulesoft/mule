/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.dsl.declaration;

import static org.mule.metadata.api.utils.MetadataTypeUtils.getLocalPart;
import static org.mule.runtime.api.component.Component.NS_MULE_DOCUMENTATION;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.app.declaration.api.fluent.ElementDeclarer.forExtension;
import static org.mule.runtime.app.declaration.api.fluent.ElementDeclarer.newListValue;
import static org.mule.runtime.app.declaration.api.fluent.ElementDeclarer.newObjectValue;
import static org.mule.runtime.app.declaration.api.fluent.ElementDeclarer.newParameterGroup;
import static org.mule.runtime.app.declaration.api.fluent.SimpleValueType.BOOLEAN;
import static org.mule.runtime.app.declaration.api.fluent.SimpleValueType.DATETIME;
import static org.mule.runtime.app.declaration.api.fluent.SimpleValueType.NUMBER;
import static org.mule.runtime.app.declaration.api.fluent.SimpleValueType.STRING;
import static org.mule.runtime.app.declaration.api.fluent.SimpleValueType.TIME;
import static org.mule.runtime.ast.api.xml.AstXmlParserAttribute.IS_CDATA;
import static org.mule.runtime.config.internal.dsl.XmlConstants.buildRawParamKeyForDocAttribute;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_CONFIGURATION;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getId;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isMap;
import static org.mule.runtime.extension.api.util.LayoutOrderComparator.OBJECTS_FIELDS_BY_LAYOUT_ORDER;
import static org.mule.runtime.extension.api.util.NameUtils.getAliasName;
import static org.mule.runtime.config.internal.dsl.utils.DslConstants.CONFIG_ATTRIBUTE_NAME;
import static org.mule.runtime.config.internal.dsl.utils.DslConstants.CORE_NAMESPACE;
import static org.mule.runtime.config.internal.dsl.utils.DslConstants.CORE_SCHEMA_LOCATION;
import static org.mule.runtime.config.internal.dsl.utils.DslConstants.KEY_ATTRIBUTE_NAME;
import static org.mule.runtime.config.internal.dsl.utils.DslConstants.VALUE_ATTRIBUTE_NAME;

import static java.lang.Thread.currentThread;
import static java.util.Collections.sort;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Predicates.alwaysTrue;

import org.mule.metadata.api.annotation.TypeIdAnnotation;
import org.mule.metadata.api.model.AnyType;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.BooleanType;
import org.mule.metadata.api.model.DateTimeType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.NumberType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.TimeType;
import org.mule.metadata.api.model.UnionType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.ComposableModel;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.construct.ConstructModel;
import org.mule.runtime.api.meta.model.nested.NestedComponentModel;
import org.mule.runtime.api.meta.model.nested.NestedRouteModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.app.declaration.api.ComponentElementDeclaration;
import org.mule.runtime.app.declaration.api.ParameterValue;
import org.mule.runtime.app.declaration.api.RouteElementDeclaration;
import org.mule.runtime.app.declaration.api.fluent.ArtifactDeclarer;
import org.mule.runtime.app.declaration.api.fluent.ComponentElementDeclarer;
import org.mule.runtime.app.declaration.api.fluent.ConfigurationElementDeclarer;
import org.mule.runtime.app.declaration.api.fluent.ConnectionElementDeclarer;
import org.mule.runtime.app.declaration.api.fluent.ConstructElementDeclarer;
import org.mule.runtime.app.declaration.api.fluent.ElementDeclarer;
import org.mule.runtime.app.declaration.api.fluent.HasNestedComponentDeclarer;
import org.mule.runtime.app.declaration.api.fluent.OperationElementDeclarer;
import org.mule.runtime.app.declaration.api.fluent.ParameterGroupElementDeclarer;
import org.mule.runtime.app.declaration.api.fluent.ParameterListValue;
import org.mule.runtime.app.declaration.api.fluent.ParameterObjectValue;
import org.mule.runtime.app.declaration.api.fluent.ParameterSimpleValue;
import org.mule.runtime.app.declaration.api.fluent.ParameterizedBuilder;
import org.mule.runtime.app.declaration.api.fluent.ParameterizedElementDeclarer;
import org.mule.runtime.app.declaration.api.fluent.RouteElementDeclarer;
import org.mule.runtime.app.declaration.api.fluent.SimpleValueType;
import org.mule.runtime.app.declaration.api.fluent.SourceElementDeclarer;
import org.mule.runtime.app.declaration.api.fluent.TopLevelParameterDeclarer;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.ast.api.MetadataTypeAdapter;
import org.mule.runtime.ast.api.xml.AstXmlParser;
import org.mule.runtime.config.api.properties.ConfigurationPropertiesHierarchyBuilder;
import org.mule.runtime.config.api.properties.ConfigurationPropertiesResolver;
import org.mule.runtime.dsl.api.xml.XmlNamespaceInfoProvider;
import org.mule.runtime.extension.api.declaration.type.annotation.FlattenedTypeAnnotation;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.runtime.module.tooling.internal.dsl.model.XmlArtifactDeclarationLoader;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;

/**
 * Implementation of a {@link XmlArtifactDeclarationLoader} that first obtains the {@link ArtifactAst} from the provided xml file
 * and then generates the declaration from that {@link ArtifactAst}.
 *
 * @since 4.4
 */
public class AstXmlArtifactDeclarationLoader implements XmlArtifactDeclarationLoader {

  private final DslResolvingContext context;

  private final AstXmlParser parser;

  public AstXmlArtifactDeclarationLoader(DslResolvingContext context) {
    this.context = context;

    ConfigurationPropertiesResolver propertyResolver = new ConfigurationPropertiesHierarchyBuilder()
        .withEnvironmentProperties()
        .withSystemProperties()
        .build();

    parser = AstXmlParser.builder().withSchemaValidationsDisabled()
        .withPropertyResolver(propertyKey -> (String) propertyResolver.resolveValue(propertyKey))
        .withExtensionModels(context.getExtensions())
        .build();
  }

  @Override
  public ArtifactDeclaration load(InputStream configResource) {
    return load("app.xml", configResource);
  }

  @Override
  public ArtifactDeclaration load(String name, InputStream configResource) {
    return declareArtifact(loadArtifactAst(name, configResource));
  }

  private ArtifactAst loadArtifactAst(String name, InputStream resource) {
    checkArgument(resource != null, "The given application was not found as resource");
    return parser.parse(name, resource);
  }

  public static List<XmlNamespaceInfoProvider> createFromPluginClassloaders(Function<ClassLoader, List<XmlNamespaceInfoProvider>> xmlNamespaceInfoProvidersSupplier,
                                                                            List<ClassLoader> pluginsClassLoaders) {
    final ImmutableList.Builder<XmlNamespaceInfoProvider> namespaceInfoProvidersBuilder = ImmutableList.builder();
    namespaceInfoProvidersBuilder
        .addAll(xmlNamespaceInfoProvidersSupplier.apply(currentThread().getContextClassLoader()));
    for (ClassLoader pluginClassLoader : pluginsClassLoaders) {
      namespaceInfoProvidersBuilder.addAll(xmlNamespaceInfoProvidersSupplier.apply(pluginClassLoader));
    }
    return namespaceInfoProvidersBuilder.build();
  }

  private ArtifactDeclaration declareArtifact(ArtifactAst artifact) {
    ArtifactDeclarer artifactDeclarer = ElementDeclarer.newArtifact();

    StringBuilder schemaLocations = new StringBuilder();

    artifactDeclarer.withCustomParameter("xmlns", CORE_NAMESPACE);
    schemaLocations.append(CORE_NAMESPACE + " " + CORE_SCHEMA_LOCATION + " ");

    // Order the namespaces by appearance order
    final List<Object> namespacesOrdered = new ArrayList<>();
    artifact.recursiveStream()
        .forEach(c -> {
          if (!namespacesOrdered.contains(c.getIdentifier().getNamespaceUri())) {
            namespacesOrdered.add(c.getIdentifier().getNamespaceUri());
          }
        });

    artifact.dependencies()
        .stream()
        .filter(em -> !em.getXmlDslModel().getNamespace().equals(CORE_NAMESPACE))
        .sorted((em1, em2) -> namespacesOrdered.indexOf(em1.getXmlDslModel().getNamespace())
            - namespacesOrdered.indexOf(em2.getXmlDslModel().getNamespace()))
        .map(ExtensionModel::getXmlDslModel)
        .forEach(dslModel -> {
          artifactDeclarer.withCustomParameter("xmlns:" + dslModel.getPrefix(), dslModel.getNamespace());
          schemaLocations.append(dslModel.getNamespace() + " " + dslModel.getSchemaLocation() + " ");
        });

    if (artifact.recursiveStream()
        .anyMatch(c -> !c.getMetadata().getDocAttributes().isEmpty())) {
      artifactDeclarer.withCustomParameter("xmlns:doc", NS_MULE_DOCUMENTATION);
    }
    artifactDeclarer.withCustomParameter("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
    artifactDeclarer.withCustomParameter("xsi:schemaLocation", schemaLocations.toString().trim());

    artifact.topLevelComponentsStream()
        .forEach(c -> declareElement(c, artifactDeclarer));

    return artifactDeclarer.getDeclaration();
  }

  private void declareElement(final ComponentAst component, final ArtifactDeclarer artifactDeclarer) {
    final ElementDeclarer extensionElementsDeclarer = forExtension(component.getExtensionModel().getName());

    component.getModel(ConstructModel.class)
        .map(model -> {
          final ConstructElementDeclarer declarer = extensionElementsDeclarer.newConstruct(model.getName());
          declareComponentModel(component, model, declarer);
          return declarer;
        })
        .ifPresent(declarer -> {
          component.getComponentId()
              .filter(id -> !OBJECT_MULE_CONFIGURATION.equals(id))
              .ifPresent(declarer::withRefName);
          artifactDeclarer.withGlobalElement(declarer.getDeclaration());
        });

    component.getModel(ConfigurationModel.class)
        .ifPresent(model -> {
          ConfigurationElementDeclarer configurationDeclarer = extensionElementsDeclarer.newConfiguration(model.getName());

          component.getComponentId().ifPresent(configurationDeclarer::withRefName);

          Map<String, String> attributes = resolveAttributes(component, param -> !param.getModel().isComponentId());

          component.directChildrenStream()
              .filter(config -> declareAsConnectionProvider(component.getExtensionModel(), model, configurationDeclarer,
                                                            config, extensionElementsDeclarer))
              .collect(toList());

          declareParameterizedComponent(model, component.getGenerationInformation().getSyntax().get(),
                                        configurationDeclarer, attributes, component);
          artifactDeclarer.withGlobalElement(configurationDeclarer.getDeclaration());
        });

    final MetadataType type = component.getType();
    if (type != null) {
      TopLevelParameterDeclarer topLevelParameter = extensionElementsDeclarer
          .newGlobalParameter(component.getIdentifier().getName());

      component.getComponentId().ifPresent(topLevelParameter::withRefName);

      type.accept(getParameterDeclarerVisitor(component, component.getGenerationInformation().getSyntax().get(),
                                              value -> topLevelParameter.withValue((ParameterObjectValue) value)));

      artifactDeclarer.withGlobalElement(topLevelParameter.getDeclaration());
    }
  }

  private void declareComponent(final Consumer<ComponentElementDeclaration> declarationConsumer,
                                final ComponentAst component,
                                final ElementDeclarer extensionElementsDeclarer) {
    component.getModel(OperationModel.class)
        .map(model -> {
          final OperationElementDeclarer declarer = extensionElementsDeclarer.newOperation(model.getName());
          declareComponentModel(component, model, declarer);
          return declarer.getDeclaration();
        })
        .ifPresent(declarationConsumer::accept);

    component.getModel(SourceModel.class)
        .map(model -> {
          final SourceElementDeclarer declarer = extensionElementsDeclarer.newSource(model.getName());
          declareComponentModel(component, model, declarer);
          return declarer.getDeclaration();
        })
        .ifPresent(declarationConsumer::accept);

    component.getModel(ConstructModel.class)
        .map(model -> {
          final ConstructElementDeclarer declarer = extensionElementsDeclarer.newConstruct(model.getName());
          declareComponentModel(component, model, declarer);
          return declarer.getDeclaration();
        })
        .ifPresent(declarationConsumer::accept);

    component.getModel(NestedComponentModel.class)
        .map(model -> {
          final ConstructElementDeclarer declarer = extensionElementsDeclarer.newConstruct(model.getName());
          declareComponentModel(component, model, declarer);
          return declarer.getDeclaration();
        })
        .ifPresent(declarationConsumer::accept);
  }

  private MetadataType getGroupParameterType(Optional<ParameterGroupModel> messageParameterGroup, String paramName) {
    MetadataType metadataType = null;

    if (messageParameterGroup.isPresent()) {
      metadataType = getGroupParameterType(messageParameterGroup.get(), paramName);
    }

    return metadataType;
  }

  private MetadataType getGroupParameterType(ParameterGroupModel model, String paramName) {
    MetadataType metadataType = null;

    Optional<ParameterModel> parameter = model.getParameter(paramName);
    if (parameter.isPresent()) {
      metadataType = model.getParameter(paramName).get().getType();
    }

    return metadataType;
  }

  private boolean declareAsConnectionProvider(ExtensionModel ownerExtension,
                                              ConfigurationModel model,
                                              ConfigurationElementDeclarer configurationDeclarer,
                                              ComponentAst component,
                                              ElementDeclarer extensionElementsDeclarer) {
    Optional<ConnectionProviderModel> connectionProvider = component.getModel(NamedObject.class)
        .flatMap(namedModel -> model.getConnectionProviderModel(namedModel.getName()));

    if (!connectionProvider.isPresent()) {
      connectionProvider = component.getModel(NamedObject.class)
          .flatMap(namedModel -> ownerExtension.getConnectionProviderModel(namedModel.getName()));
    }

    if (!connectionProvider.isPresent()) {
      return true;
    }

    ConnectionProviderModel providerModel = connectionProvider.get();
    ConnectionElementDeclarer connectionDeclarer = extensionElementsDeclarer.newConnection(providerModel.getName());
    declareParameterizedComponent(providerModel, component.getGenerationInformation().getSyntax().get(), connectionDeclarer,
                                  resolveAttributes(component, alwaysTrue()),
                                  component);

    configurationDeclarer.withConnection(connectionDeclarer.getDeclaration());
    return false;
  }

  private void declareComponentModel(ComponentAst component,
                                     ComponentModel model,
                                     ComponentElementDeclarer declarer) {
    final ComponentParameterAst parameter = component.getParameter(DEFAULT_GROUP_NAME, CONFIG_ATTRIBUTE_NAME);
    if (parameter != null) {
      declarer.withConfig(parameter.getResolvedRawValue());
    }

    final DslElementSyntax elementDsl = component.getGenerationInformation().getSyntax().get();

    model.getParameterGroupModels()
        .forEach(group -> declareParameterGroup(component, model, declarer, elementDsl, group,
                                                model.getParameterGroupModels().get(0) == group,
                                                pm -> component.getParameter(group.getName(), pm.getName())));

    if (model instanceof SourceModel) {
      ((SourceModel) model).getSuccessCallback()
          .ifPresent(callbackModel -> callbackModel.getParameterGroupModels()
              .forEach(group -> declareParameterGroup(component, model, declarer, elementDsl, group, false,
                                                      pm -> component.getParameter(group.getName(), pm.getName()))));

      ((SourceModel) model).getErrorCallback()
          .ifPresent(callbackModel -> callbackModel.getParameterGroupModels()
              .forEach(group -> declareParameterGroup(component, model, declarer, elementDsl, group, false,
                                                      pm -> component.getParameter(group.getName(), pm.getName()))));
    }

    declareComposableModel(model, elementDsl, component, declarer);
  }

  private void declareParameterGroup(ComponentAst component, ComponentModel model, ComponentElementDeclarer declarer,
                                     final DslElementSyntax elementDsl,
                                     ParameterGroupModel group, boolean processDocAttributes,
                                     final Function<? super ParameterModel, ? extends ComponentParameterAst> mapper) {
    final List<ComponentParameterAst> groupParams = group.getParameterModels()
        .stream()
        .map(mapper)
        .filter(p -> p != null)
        .collect(toList());

    ParameterGroupElementDeclarer groupDeclarer = newParameterGroup(group.getName());
    final Map<String, ComponentParameterAst> groupAttributes =
        resolveParams(component, param -> groupParams.contains(param));

    if (group.isShowInDsl()) {
      if (groupParams
          .stream()
          .anyMatch(p -> p.getValue().getValue().isPresent())) {
        declareInlineGroup(component, elementDsl, group, processDocAttributes, groupParams, groupDeclarer, groupAttributes);
      }
    } else {
      copyExplicitAttributeParams(groupAttributes, declarer, model);
      if (processDocAttributes) {
        copyExplicitAttributes(model, resolveDocAttributes(component), declarer);
      }

      declareNonInlineParameterGroup(declarer, component, elementDsl, group, groupDeclarer);
    }
    if (!groupDeclarer.getDeclaration().getParameters().isEmpty()) {
      declarer.withParameterGroup(groupDeclarer.getDeclaration());
    }
  }

  private void declareNonInlineParameterGroup(ParameterizedElementDeclarer<?, ?> declarer, ComponentAst paramsOwner,
                                              final DslElementSyntax elementDsl, ParameterGroupModel group,
                                              ParameterGroupElementDeclarer groupDeclarer) {
    group.getParameterModels()
        .stream()
        .forEach(param -> elementDsl.getChild(param.getName())
            .ifPresent(paramDsl -> {
              final Object paramValue = paramsOwner.getParameter(group.getName(), param.getName()).getValue().getRight();

              if (paramValue == null) {
                return;
              }

              param.getType()
                  .accept(getParameterDeclarerVisitor(paramValue,
                                                      paramDsl,
                                                      value -> groupDeclarer.withParameter(param.getName(),
                                                                                           value)));
            }));
  }

  private Map<String, String> resolveAttributes(ComponentAst component, Predicate<ComponentParameterAst> additionalFilter) {
    return resolveAttributes(component, additionalFilter, true);
  }

  private Map<String, String> resolveAttributes(ComponentAst component, Predicate<ComponentParameterAst> additionalFilter,
                                                boolean processDocAttributes) {
    Map<String, String> attributes = component.getParameters()
        .stream()
        .filter(param -> param.getRawValue() != null)
        .filter(additionalFilter)
        .collect(toMap(param -> param.getModel().getName(), param -> param.getRawValue()));

    if (processDocAttributes) {
      attributes.putAll(resolveDocAttributes(component));
    }

    return attributes;
  }

  private Map<String, ComponentParameterAst> resolveParams(ComponentAst component,
                                                           Predicate<ComponentParameterAst> additionalFilter) {
    return component.getParameters()
        .stream()
        .filter(param -> param.getRawValue() != null)
        .filter(additionalFilter)
        .collect(toMap(param -> param.getModel().getName(), param -> param));
  }

  private Map<String, String> resolveDocAttributes(ComponentAst component) {
    Map<String, String> attributes = new HashMap<>();

    component.getMetadata().getDocAttributes().entrySet().stream()
        .forEach(docAttr -> buildRawParamKeyForDocAttribute(docAttr)
            .ifPresent(key -> attributes.put(key, docAttr.getValue())));

    return attributes;
  }

  private void declareComposableModel(ComposableModel model, DslElementSyntax elementDsl,
                                      ComponentAst composableComponent, HasNestedComponentDeclarer declarer) {
    composableComponent.directChildrenStream().forEach(child -> {
      ElementDeclarer extensionElementsDeclarer = forExtension(child.getExtensionModel().getName());

      Reference<Boolean> componentFound = new Reference<>(false);

      declareRoute(model, elementDsl, child, extensionElementsDeclarer)
          .ifPresent(route -> {
            declarer.withComponent(route);
            componentFound.set(true);
          });

      if (!componentFound.get()) {
        declareComponent(declaration -> declarer.withComponent(declaration), child, extensionElementsDeclarer);
      }
    });
  }

  private Optional<RouteElementDeclaration> declareRoute(ComposableModel model, DslElementSyntax elementDsl,
                                                         ComponentAst child,
                                                         ElementDeclarer extensionElementsDeclarer) {
    return model.getNestedComponents().stream()
        .filter(nestedModel -> child.getModel(NestedRouteModel.class)
            .map(nem -> nem.equals(nestedModel))
            .orElse(false))
        .findFirst()
        .map(nestedModel -> {
          RouteElementDeclarer routeDeclarer = extensionElementsDeclarer.newRoute(nestedModel.getName());
          DslElementSyntax routeDsl = elementDsl.getContainedElement(nestedModel.getName()).get();

          Map<String, String> attributes = resolveAttributes(child, alwaysTrue());

          declareParameterizedComponent(nestedModel, routeDsl, routeDeclarer, attributes, child);
          declareComposableModel(nestedModel, elementDsl, child, routeDeclarer);
          return routeDeclarer.getDeclaration();
        });
  }


  private void declareParameterizedComponent(ParameterizedModel model, DslElementSyntax elementDsl,
                                             ParameterizedElementDeclarer declarer,
                                             Map<String, String> rawParams,
                                             ComponentAst component) {
    copyExplicitAttributes(model, rawParams, declarer);
    declareChildParameters(model, elementDsl, component, declarer);
  }

  private void declareChildParameters(ParameterizedModel model, DslElementSyntax modelDsl, ComponentAst component,
                                      ParameterizedElementDeclarer declarer) {

    model.getParameterGroupModels()
        .forEach(group -> {
          ParameterGroupElementDeclarer groupDeclarer = newParameterGroup(group.getName());

          if (group.isShowInDsl()) {
            final List<ComponentParameterAst> groupParams = group.getParameterModels()
                .stream()
                .map(pm -> component.getParameter(group.getName(), pm.getName()))
                .filter(p -> p != null)
                .collect(toList());

            if (groupParams
                .stream()
                .anyMatch(p -> p.getValue().getValue().isPresent())) {
              declareInlineGroup(component,
                                 component.getGenerationInformation().getSyntax().get(),
                                 group, true, groupParams, groupDeclarer,
                                 resolveParams(component, param -> groupParams.contains(param)));
            }
          } else {
            declareNonInlineParameterGroup(declarer, component, modelDsl, group, groupDeclarer);
          }

          if (!groupDeclarer.getDeclaration().getParameters().isEmpty()) {
            declarer.withParameterGroup(groupDeclarer.getDeclaration());
          }
        });
  }

  private void declareInlineGroup(ComponentAst component, final DslElementSyntax elementDsl, ParameterGroupModel group,
                                  boolean processDocAttributes, final List<ComponentParameterAst> groupParams,
                                  ParameterGroupElementDeclarer groupDeclarer,
                                  final Map<String, ComponentParameterAst> groupAttributes) {
    copyExplicitAttributeParams(groupAttributes, groupDeclarer, group);
    if (processDocAttributes) {
      copyExplicitAttributes(resolveDocAttributes(component), groupDeclarer, group);
    }
    declareComplexParameterValue(group, elementDsl.getChild(group.getName()).get(),
                                 groupParams.stream()
                                     .filter(p -> p.getValue() != null && p.getValue().isRight()
                                         && p.getValue().getRight() instanceof ComponentAst),
                                 groupDeclarer);
  }

  private void declareComplexParameterValue(ParameterGroupModel group,
                                            DslElementSyntax groupDsl,
                                            final Stream<ComponentParameterAst> groupChildren,
                                            ParameterizedBuilder<String, ParameterValue, ?> groupBuilder) {

    groupChildren
        .forEach(child -> {
          group.getParameterModels().stream()
              .filter(paramModel -> paramModel.equals(child.getModel()))
              .findFirst()
              .ifPresent(param -> param.getType()
                  .accept(getParameterDeclarerVisitor(child.getValue().getRight(),
                                                      groupDsl.getChild(param.getName()).get(),
                                                      value -> groupBuilder.withParameter(param.getName(), value))));
        });
  }

  private MetadataTypeVisitor getParameterDeclarerVisitor(final Object paramValue,
                                                          final DslElementSyntax paramDsl,
                                                          final Consumer<ParameterValue> valueConsumer) {
    return new MetadataTypeVisitor() {

      @Override
      public void visitArrayType(ArrayType arrayType) {
        ParameterListValue.Builder listBuilder = newListValue();

        ((Collection<ComponentAst>) (paramValue))
            .forEach(item -> arrayType.getType().accept(getParameterDeclarerVisitor(item,
                                                                                    paramDsl.getGeneric(arrayType.getType())
                                                                                        .get(),
                                                                                    listBuilder::withValue)));
        valueConsumer.accept(listBuilder.build());
      }

      @Override
      public void visitObject(ObjectType objectType) {
        ParameterObjectValue.Builder objectValue = newObjectValue();
        if (isMap(objectType)) {
          createMapValue(objectValue,
                         (Collection<ComponentAst>) paramValue,
                         objectType.getOpenRestriction().orElse(null));
        } else {
          if (paramDsl.isWrapped()) {
            createWrappedObject(objectType, objectValue, (ComponentAst) paramValue);
          } else {
            createObjectValueFromType(objectType, objectValue, (ComponentAst) paramValue, paramDsl);
          }
        }
        valueConsumer.accept(objectValue.build());
      }

      @Override
      public void visitUnion(UnionType unionType) {
        final MetadataType actualType =
            getChildMetadataTypeFromUnion(unionType, ((ComponentAst) paramValue).getIdentifier().getName());
        visitObject((ObjectType) actualType);
      }
    };
  }

  private void createMapValue(ParameterObjectValue.Builder objectValue, Collection<ComponentAst> items,
                              MetadataType elementMetadataType) {
    items
        .forEach(comp -> {
          final ComponentParameterAst keyParam = comp.getParameter(DEFAULT_GROUP_NAME, KEY_ATTRIBUTE_NAME);
          final ComponentParameterAst valueParam = comp.getParameter(DEFAULT_GROUP_NAME, VALUE_ATTRIBUTE_NAME);

          if (keyParam != null && keyParam.getRawValue() != null && valueParam != null && valueParam.getRawValue() != null) {
            objectValue.withParameter(keyParam.getRawValue(),
                                      createParameterSimpleValue(valueParam.getRawValue(), elementMetadataType));
          }
        });
  }

  private void createWrappedObject(ObjectType objectType, ParameterObjectValue.Builder objectValue, ComponentAst wrappedConfig) {
    Set<ObjectType> subTypes = context.getTypeCatalog().getSubTypes(objectType);
    if (!subTypes.isEmpty()) {
      subTypes.stream()
          .filter(subType -> wrappedConfig.getModel(MetadataTypeAdapter.class)
              .map(mtma -> mtma.isWrapperFor(subType))
              .orElse(false))
          .findFirst()
          .ifPresent(subType -> createObjectValueFromType(subType, objectValue, wrappedConfig,
                                                          wrappedConfig.getGenerationInformation().getSyntax().get()));

    } else {
      createObjectValueFromType(objectType, objectValue, wrappedConfig,
                                wrappedConfig.getGenerationInformation().getSyntax().get());
    }
  }

  private void createObjectValueFromType(ObjectType objectType, ParameterObjectValue.Builder objectValue, ComponentAst component,
                                         DslElementSyntax paramDsl) {

    getId(objectType)
        // Do not put typeId for inners of ee:transform, just to keep compatibility with previous implementation
        .filter(id -> !(id.equals("SetPayload") || id.equals("SetAttributes") || id.equals("SetVariable")))
        .ifPresent(objectValue::ofType);

    final ComponentParameterAst configRefParam = component.getParameter(getAliasName(objectType), CONFIG_ATTRIBUTE_NAME);
    if (configRefParam != null && configRefParam.getRawValue() != null) {
      objectValue.withParameter(CONFIG_ATTRIBUTE_NAME,
                                ParameterSimpleValue.of(configRefParam.getRawValue(), STRING));
    }

    copyExplicitAttributes(resolveAttributes(component, param -> !param.getModel().isComponentId()), objectValue, objectType);

    final List<ObjectFieldType> fields = new ArrayList<>(objectType.getFields());
    sort(fields, OBJECTS_FIELDS_BY_LAYOUT_ORDER);

    fields.forEach(fieldType -> {
      final ComponentParameterAst param = component.getParameter(resolveGroupName(objectType), getLocalPart(fieldType));
      if (param != null && param.getValue().getRight() != null && param.getValue().getRight() instanceof ComponentAst) {
        fieldType.getValue().accept(getParameterDeclarerVisitor(param.getValue().getRight(),
                                                                paramDsl
                                                                    .getContainedElement(getLocalPart(fieldType))
                                                                    .get(),
                                                                fieldValue -> objectValue
                                                                    .withParameter(getLocalPart(fieldType),
                                                                                   fieldValue)));
      }
    });
  }

  private String resolveGroupName(ObjectType objectType) {
    String aliasName;
    try {
      aliasName = getAliasName(objectType);
    } catch (IllegalArgumentException e) {
      aliasName = DEFAULT_GROUP_NAME;
    }
    return aliasName;
  }

  private MetadataType getChildMetadataType(MetadataType parentMetadataType, String modelParamName) {
    return getChildMetadataType(parentMetadataType, modelParamName, modelParamName);
  }

  private MetadataType getChildMetadataType(MetadataType parentMetadataType, String configParamName, String modelParamName) {
    MetadataType childMetadataType;

    if (parentMetadataType instanceof ObjectFieldType) {
      parentMetadataType = ((ObjectFieldType) parentMetadataType).getValue();
    }

    if (parentMetadataType instanceof ObjectType) {
      childMetadataType = getMetadataTypeFromObjectType((ObjectType) parentMetadataType, configParamName, modelParamName);
    } else if (parentMetadataType instanceof UnionType) {
      childMetadataType = getChildMetadataTypeFromUnion((UnionType) parentMetadataType, modelParamName);
    } else if (parentMetadataType instanceof ArrayType) {
      childMetadataType = getChildMetadataType(((ArrayType) parentMetadataType).getType(), modelParamName, configParamName);
    } else {
      throw new IllegalStateException("Cannot obtain child parameter type from " + parentMetadataType.getClass().getName());
    }

    return childMetadataType;
  }

  private MetadataType getMetadataTypeFromObjectType(ObjectType objectMetadataType, String configParamName,
                                                     String modelParamName) {
    MetadataType childMetadataType;
    Optional<ObjectFieldType> fieldByName = objectMetadataType.getFieldByName(configParamName);

    if (fieldByName.isPresent()) {
      ObjectFieldType objectFieldType = fieldByName.get();

      if (objectFieldType.getValue() instanceof ObjectType
          || objectFieldType.getValue() instanceof AnyType) {
        childMetadataType = objectFieldType;
      } else if (objectFieldType.getValue() instanceof org.mule.metadata.api.model.SimpleType) {
        childMetadataType = objectFieldType.getValue();
      } else if (objectFieldType.getValue() instanceof UnionType) {
        childMetadataType = getChildMetadataTypeFromUnion((UnionType) objectFieldType.getValue(), modelParamName);
      } else {
        throw new IllegalStateException("Unsupported attribute type: " + objectFieldType.getValue().getClass().getName());
      }
    } else {
      childMetadataType = getMetadataTypeFromFlattenedFields(objectMetadataType, modelParamName);
    }
    return childMetadataType;
  }

  private MetadataType getChildMetadataTypeFromUnion(UnionType parentMetadataType, String modelParamName) {
    Optional<MetadataType> result = parentMetadataType.getTypes().stream()
        .filter(metadataType -> metadataType.getAnnotation(TypeIdAnnotation.class).get().getValue().equals(modelParamName))
        .findFirst();

    return result.orElse(null);
  }

  private MetadataType getMetadataTypeFromFlattenedFields(ObjectType objectMetadataType, String modelParamName) {
    MetadataType childMetadataType = null;

    List<MetadataType> flattenedFieldTypes =
        objectMetadataType.getFields().stream().filter(field -> field.getAnnotation(FlattenedTypeAnnotation.class).isPresent())
            .map(ObjectFieldType::getValue).collect(toList());

    for (MetadataType flattenedFieldType : flattenedFieldTypes) {
      for (ObjectFieldType field : ((ObjectType) flattenedFieldType).getFields()) {
        if (field.getKey().getName().getLocalPart().equals(modelParamName)) {
          childMetadataType = field.getValue();
          break;
        }
      }

      if (childMetadataType != null) {
        break;
      }
    }
    return childMetadataType;
  }

  private void cloneAsDeclaration(ComponentAst component, ParameterObjectValue.Builder objectValue, MetadataType metadataType) {
    try {
      copyExplicitAttributes(resolveAttributes(component, param -> !param.getModel().isComponentId()), objectValue, metadataType);
      copyChildren(component, objectValue, metadataType);
    } catch (IllegalStateException e) {
      throw e;
    }
  }

  private void copyExplicitAttributes(Map<String, String> attributes,
                                      ParameterizedBuilder<String, ParameterValue, ?> builder,
                                      ParameterGroupModel group) {
    attributes.entrySet().stream()
        .filter(e -> !e.getKey().equals(CONFIG_ATTRIBUTE_NAME))
        .forEach(e -> builder
            .withParameter(e.getKey(),
                           createParameterSimpleValue(e.getValue(), getGroupParameterType(group, e.getKey()))));
  }

  private void copyExplicitAttributes(Map<String, String> attributes,
                                      ParameterizedBuilder<String, ParameterValue, ?> builder, MetadataType parentMetadataType) {
    attributes.entrySet().stream()
        .filter(e -> !e.getKey().equals(CONFIG_ATTRIBUTE_NAME))
        .forEach(e -> builder.withParameter(e.getKey(), createParameterSimpleValue(e.getValue(),
                                                                                   getChildMetadataType(parentMetadataType,
                                                                                                        e.getKey()))));
  }

  private void copyExplicitAttributes(ParameterizedModel model,
                                      Map<String, String> attributes,
                                      ParameterizedElementDeclarer builder) {
    attributes.entrySet().stream()
        .filter(e -> !e.getKey().equals(CONFIG_ATTRIBUTE_NAME))
        .forEach(e -> {
          Optional<ParameterGroupModel> ownerGroup = model.getParameterGroupModels().stream()
              .filter(group -> group.getParameter(e.getKey()).isPresent())
              .findFirst();
          if (ownerGroup.isPresent()) {
            builder
                .withParameterGroup(newParameterGroup(ownerGroup.get().getName())
                    .withParameter(e.getKey(), createParameterSimpleValue(e.getValue(),
                                                                          getGroupParameterType(ownerGroup, e.getKey())))
                    .getDeclaration());
          } else {
            builder.withCustomParameter(e.getKey(), e.getValue());
          }
        });
  }

  private void copyExplicitAttributeParams(Map<String, ComponentParameterAst> attributes,
                                           ParameterizedBuilder<String, ParameterValue, ?> builder,
                                           ParameterGroupModel group) {
    attributes.entrySet().stream()
        .filter(e -> !e.getKey().equals(CONFIG_ATTRIBUTE_NAME))
        .forEach(e -> builder
            .withParameter(e.getKey(),
                           e.getValue().getMetadata().map(m -> IS_CDATA.get(m).orElse(false))
                               .orElse(false)
                                   ? createParameterSimpleCdataValue(e.getValue().getRawValue(),
                                                                     getGroupParameterType(group, e.getKey()))
                                   : createParameterSimpleValue(e.getValue().getRawValue(),
                                                                getGroupParameterType(group, e.getKey()))));
  }

  private void copyExplicitAttributeParams(Map<String, ComponentParameterAst> attributes,
                                           ParameterizedElementDeclarer builder,
                                           ParameterizedModel model) {
    attributes.entrySet().stream()
        .filter(e -> !e.getKey().equals(CONFIG_ATTRIBUTE_NAME))
        .forEach(e -> {
          Optional<ParameterGroupModel> ownerGroup = model.getParameterGroupModels().stream()
              .filter(group -> group.getParameter(e.getKey()).isPresent())
              .findFirst();
          if (ownerGroup.isPresent()) {
            builder
                .withParameterGroup(newParameterGroup(ownerGroup.get().getName())
                    .withParameter(e.getKey(), createParameterSimpleValue(e.getValue().getRawValue(),
                                                                          getGroupParameterType(ownerGroup, e.getKey())))
                    .getDeclaration());
          } else {
            builder.withCustomParameter(e.getKey(), e.getValue().getRawValue());
          }
        });
  }

  private void copyChildren(ComponentAst component, ParameterizedBuilder<String, ParameterValue, ?> builder,
                            MetadataType metadataType) {
    component.directChildrenStream().forEach(child -> {
      ParameterObjectValue.Builder childBuilder = newObjectValue();
      MetadataType childMetadataType = getChildMetadataType(metadataType, child.getIdentifier().getName());
      cloneAsDeclaration(child, childBuilder, childMetadataType);
      builder.withParameter(child.getIdentifier().getName(), childBuilder.build());
    });
  }

  private ParameterValue createParameterSimpleValue(String a, MetadataType type) {
    return ParameterSimpleValue.of(a, getSimpleTypeFromMetadataType(type));
  }

  private ParameterValue createParameterSimpleCdataValue(String a, MetadataType type) {
    return ParameterSimpleValue.cdata(a, getSimpleTypeFromMetadataType(type));
  }

  private SimpleValueType getSimpleTypeFromMetadataType(MetadataType type) {
    if (type instanceof DateTimeType) {
      return DATETIME;
    } else if (type instanceof TimeType) {
      return TIME;
    } else if (type instanceof BooleanType) {
      return BOOLEAN;
    } else if (type instanceof NumberType) {
      return NUMBER;
    } else {
      return STRING;
    }
  }
}
