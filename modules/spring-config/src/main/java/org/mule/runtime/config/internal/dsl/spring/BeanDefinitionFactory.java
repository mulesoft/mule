/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import static java.lang.String.format;
import static java.util.Optional.of;
import static org.mule.runtime.api.component.AbstractComponent.LOCATION_KEY;
import static org.mule.runtime.api.component.Component.NS_MULE_DOCUMENTATION;
import static org.mule.runtime.api.component.Component.Annotations.NAME_ANNOTATION_KEY;
import static org.mule.runtime.api.component.Component.Annotations.REPRESENTATION_ANNOTATION_KEY;
import static org.mule.runtime.api.serialization.ObjectSerializer.DEFAULT_OBJECT_SERIALIZER_NAME;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.CONFIGURATION_IDENTIFIER;
import static org.mule.runtime.config.api.dsl.model.ComponentBuildingDefinitionRegistry.WrapperElementType.SINGLE;
import static org.mule.runtime.config.internal.dsl.spring.CommonBeanDefinitionCreator.areMatchingTypes;
import static org.mule.runtime.config.internal.dsl.spring.ComponentModelHelper.addAnnotation;
import static org.mule.runtime.config.internal.model.ApplicationModel.ANNOTATIONS_ELEMENT_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.DESCRIPTION_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.DOC_DESCRIPTION_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.GLOBAL_PROPERTY_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.MULE_PROPERTIES_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.MULE_PROPERTY_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.OBJECT_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.SECURITY_MANAGER_IDENTIFIER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_EXPRESSION_LANGUAGE;
import static org.mule.runtime.core.internal.component.ComponentAnnotations.ANNOTATION_COMPONENT_CONFIG;
import static org.mule.runtime.core.internal.component.ComponentAnnotations.ANNOTATION_NAME;
import static org.mule.runtime.core.internal.component.ComponentAnnotations.ANNOTATION_PARAMETERS;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;
import static org.mule.runtime.internal.dsl.DslConstants.NAME_ATTRIBUTE_NAME;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentMetadataAst;
import org.mule.runtime.config.api.dsl.model.ComponentBuildingDefinitionRegistry;
import org.mule.runtime.config.api.dsl.model.properties.ConfigurationPropertiesProviderFactory;
import org.mule.runtime.config.internal.SpringConfigurationComponentLocator;
import org.mule.runtime.config.internal.dsl.model.SpringComponentModel;
import org.mule.runtime.core.internal.el.mvel.MVELExpressionLanguage;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

import com.google.common.collect.ImmutableSet;

/**
 * The {@code BeanDefinitionFactory} is the one that knows how to convert a {@code ComponentModel} to an actual
 * {@link org.springframework.beans.factory.config.BeanDefinition} that can later be converted to a runtime object that will be
 * part of the artifact.
 * <p>
 * It will recursively process a {@code ComponentModel} to create a {@code BeanDefinition}. For the time being it will collaborate
 * with the old bean definitions parsers for configurations that are partially defined in the new parsing method.
 *
 * @since 4.0
 */
public class BeanDefinitionFactory {

  public static final String SPRING_PROTOTYPE_OBJECT = "prototype";
  public static final String SPRING_SINGLETON_OBJECT = "singleton";
  public static final String SOURCE_TYPE = "sourceType";
  public static final String TARGET_TYPE = "targetType";
  public static final String CORE_ERROR_NS = CORE_PREFIX.toUpperCase();

  private final ImmutableSet<ComponentIdentifier> ignoredMuleCoreComponentIdentifiers =
      ImmutableSet.<ComponentIdentifier>builder()
          .add(DESCRIPTION_IDENTIFIER)
          .add(ANNOTATIONS_ELEMENT_IDENTIFIER)
          .add(DOC_DESCRIPTION_IDENTIFIER)
          .add(GLOBAL_PROPERTY_IDENTIFIER)
          .build();

  private final Set<ComponentIdentifier> ignoredMuleExtensionComponentIdentifiers;

  /**
   * These are the set of current language construct that have specific bean definitions parsers since we don't want to include
   * them in the parsing API.
   */
  private final ImmutableSet<ComponentIdentifier> customBuildersComponentIdentifiers =
      ImmutableSet.<ComponentIdentifier>builder()
          .add(MULE_PROPERTIES_IDENTIFIER)
          .add(MULE_PROPERTY_IDENTIFIER)
          .add(OBJECT_IDENTIFIER)
          .build();


  private final String artifactId;
  private final ComponentBuildingDefinitionRegistry componentBuildingDefinitionRegistry;
  private final BeanDefinitionCreator componentModelProcessor;
  private final ObjectFactoryClassRepository objectFactoryClassRepository = new ObjectFactoryClassRepository();

  /**
   * @param componentBuildingDefinitionRegistry a registry with all the known {@code ComponentBuildingDefinition}s by the
   *                                            artifact.
   * @param errorTypeRepository
   */
  public BeanDefinitionFactory(String artifactId, ComponentBuildingDefinitionRegistry componentBuildingDefinitionRegistry) {
    this.artifactId = artifactId;
    this.componentBuildingDefinitionRegistry = componentBuildingDefinitionRegistry;
    this.componentModelProcessor = buildComponentModelProcessorChainOfResponsability();
    this.ignoredMuleExtensionComponentIdentifiers = new HashSet<>();

    registerConfigurationPropertyProviders();
  }

  private void registerConfigurationPropertyProviders() {
    ServiceLoader<ConfigurationPropertiesProviderFactory> providerFactories =
        java.util.ServiceLoader.load(ConfigurationPropertiesProviderFactory.class);
    providerFactories.forEach(service -> {
      ignoredMuleExtensionComponentIdentifiers.add(service.getSupportedComponentIdentifier());
    });
  }

  public boolean isComponentIgnored(ComponentIdentifier identifier) {
    return ignoredMuleCoreComponentIdentifiers.contains(identifier) ||
        ignoredMuleExtensionComponentIdentifiers.contains(identifier);
  }

  /**
   * Creates a {@code BeanDefinition} for the {@code ComponentModel}.
   *
   * @param springComponentModels a {@link Map} created {@link ComponentAst} and {@link SpringComponentModel}
   * @param parentComponentModel        the container of the component model from which we want to create the bean definition.
   * @param componentModel              the component model from which we want to create the bean definition.
   * @param registry                    the bean registry since it may be required to get other bean definitions to create this one or to register
   *                                    the bean definition.
   * @param componentLocator            where the locations of any {@link Component}'s locations must be registered
   */
  public void resolveComponent(Map<ComponentAst, SpringComponentModel> springComponentModels,
                               ComponentAst parentComponentModel,
                               ComponentAst componentModel,
                               BeanDefinitionRegistry registry,
                               SpringConfigurationComponentLocator componentLocator) {
    if (isComponentIgnored(componentModel.getIdentifier())) {
      return;
    }

    resolveComponentBeanDefinition(springComponentModels, parentComponentModel, componentModel)
        .ifPresent(springComponentModel -> {
          springComponentModels.put(componentModel, springComponentModel);

          // TODO MULE-9638: Once we migrate all core definitions we need to define a mechanism for customizing
          // how core constructs are processed.
          processMuleConfiguration(springComponentModels, componentModel, registry);
          processMuleSecurityManager(springComponentModels, componentModel, registry);

          componentBuildingDefinitionRegistry.getBuildingDefinition(componentModel.getIdentifier())
              .ifPresent(componentBuildingDefinition -> {
                if ((springComponentModel.getType() != null)
                    && Component.class.isAssignableFrom(springComponentModel.getType())) {
                  addAnnotation(ANNOTATION_NAME, componentModel.getIdentifier(), springComponentModel);
                  // We need to use a mutable map since spring will resolve the properties placeholder present in the value if
                  // needed and it will be done by mutating the same map.

                  final Map<String, String> rawParams = new HashMap<>();
                  componentModel.getMetadata().getDocAttributes().entrySet().stream()
                      .forEach(docAttr -> buildRawParamKeyForDocAttribute(docAttr)
                          .ifPresent(key -> rawParams.put(key, docAttr.getValue())));

                  addAnnotation(ANNOTATION_PARAMETERS,
                                componentModel.getModel(ParameterizedModel.class)
                                    .map(pm -> {
                                      componentModel.getParameters().stream()
                                          .filter(param -> param.getRawValue() != null)
                                          .forEach(param -> rawParams.put(param.getModel().getName(), param.getRawValue()));

                                      return rawParams;
                                    })
                                    .orElse(rawParams),
                                springComponentModel);

                  componentLocator.addComponentLocation(componentModel.getLocation());
                  addAnnotation(ANNOTATION_COMPONENT_CONFIG, componentModel, springComponentModel);
                }
              });

          addAnnotation(LOCATION_KEY, componentModel.getLocation(), springComponentModel);
          addAnnotation(REPRESENTATION_ANNOTATION_KEY, resolveProcessorRepresentation(artifactId,
                                                                                      componentModel
                                                                                          .getLocation(),
                                                                                      componentModel
                                                                                          .getMetadata()),
                        springComponentModel);
        });
  }

  private Optional<String> buildRawParamKeyForDocAttribute(Entry<String, String> docAttr) {
    final QName qName = QName.valueOf(docAttr.getKey());

    // The doc: prefix is hard-coded here to maintain compatibility of interception api use cases where the doc
    // parameters are queried with this prefix
    if (NS_MULE_DOCUMENTATION.equals(qName.getNamespaceURI())) {
      return of("doc:" + qName.getLocalPart());
    } else if (StringUtils.isEmpty(qName.getNamespaceURI())) {
      return of("doc:" + docAttr.getKey());
    } else {
      return Optional.empty();
    }
  }

  /**
   * Generates a representation of a flow element to be logged in a standard way.
   *
   * @param appId
   * @param processorPath
   * @param element
   * @return
   */
  public static String resolveProcessorRepresentation(String appId, ComponentLocation processorPath,
                                                      ComponentMetadataAst metadata) {
    StringBuilder stringBuilder = new StringBuilder();

    stringBuilder.append(processorPath.getLocation())
        .append(" @ ")
        .append(appId);

    String sourceFile = metadata.getFileName().orElse(null);
    if (sourceFile != null) {
      stringBuilder.append(":")
          .append(sourceFile)
          .append(":")
          .append(metadata.getStartLine().orElse(-1));
    }

    Object docName = metadata.getDocAttributes().get(NAME_ANNOTATION_KEY.getLocalPart());
    if (docName != null) {
      stringBuilder.append(" (")
          .append(docName)
          .append(")");
    }

    return stringBuilder.toString();
  }

  private void processMuleConfiguration(Map<ComponentAst, SpringComponentModel> springComponentModels,
                                        ComponentAst componentModel, BeanDefinitionRegistry registry) {
    if (componentModel.getIdentifier().equals(CONFIGURATION_IDENTIFIER)) {
      AtomicReference<BeanDefinition> expressionLanguage = new AtomicReference<>();

      componentModel.directChildrenStream()
          .map(springComponentModels::get)
          .filter(childSpringComponentModel -> areMatchingTypes(MVELExpressionLanguage.class,
                                                                childSpringComponentModel.getType()))
          .forEach(childSpringComponentModel -> expressionLanguage.set(childSpringComponentModel.getBeanDefinition()));
      componentModel.getRawParameterValue("defaultObjectSerializer-ref")
          .ifPresent(defaultObjectSerializer -> {
            if (defaultObjectSerializer != DEFAULT_OBJECT_SERIALIZER_NAME) {
              registry.removeBeanDefinition(DEFAULT_OBJECT_SERIALIZER_NAME);
              registry.registerAlias(defaultObjectSerializer, DEFAULT_OBJECT_SERIALIZER_NAME);
            }
          });
      if (expressionLanguage.get() != null) {
        registry.registerBeanDefinition(OBJECT_EXPRESSION_LANGUAGE, expressionLanguage.get());
      }
    }
  }

  private void processMuleSecurityManager(Map<ComponentAst, SpringComponentModel> springComponentModels,
                                          ComponentAst componentModel, BeanDefinitionRegistry registry) {
    if (componentModel.getIdentifier().equals(SECURITY_MANAGER_IDENTIFIER)) {
      componentModel.directChildrenStream().forEach(childComponentModel -> {
        String identifier = childComponentModel.getIdentifier().getName();
        if (identifier.equals("password-encryption-strategy")
            || identifier.equals("secret-key-encryption-strategy")) {
          registry.registerBeanDefinition(childComponentModel.getRawParameterValue(NAME_ATTRIBUTE_NAME).get(),
                                          springComponentModels.get(childComponentModel).getBeanDefinition());
        }
      });
    }
  }


  private Optional<SpringComponentModel> resolveComponentBeanDefinition(Map<ComponentAst, SpringComponentModel> springComponentModels,
                                                                        ComponentAst parentComponentModel,
                                                                        ComponentAst componentModel) {
    Optional<ComponentBuildingDefinition<?>> buildingDefinitionOptional =
        componentBuildingDefinitionRegistry.getBuildingDefinition(componentModel.getIdentifier());
    if (buildingDefinitionOptional.isPresent() || customBuildersComponentIdentifiers.contains(componentModel.getIdentifier())) {
      final CreateBeanDefinitionRequest request = new CreateBeanDefinitionRequest(parentComponentModel, componentModel,
                                                                                  buildingDefinitionOptional.orElse(null));
      request.getSpringComponentModel().setType(request.retrieveTypeVisitor().getType());
      this.componentModelProcessor.processRequest(springComponentModels, request);
      return of(request.getSpringComponentModel());
    } else {
      return processComponentWrapper(springComponentModels, componentModel);
    }
  }

  private Optional<SpringComponentModel> processComponentWrapper(Map<ComponentAst, SpringComponentModel> springComponentModels,
                                                                 ComponentAst componentModel) {
    return componentBuildingDefinitionRegistry.getWrappedComponent(componentModel.getIdentifier())
        .map(wrapperElementType -> {
          if (wrapperElementType.equals(SINGLE)) {
            if (componentModel.directChildrenStream().count() == 0) {
              String location =
                  componentModel.getLocation() != null ? componentModel.getLocation().getLocation() : "";
              throw new IllegalStateException(format("Element [%s] located at [%s] does not have any child element declared, but one is required.",
                                                     componentModel.getIdentifier(), location));
            }
            final SpringComponentModel firstSpringComponentModel =
                springComponentModels.get(componentModel.directChildrenStream().findFirst().get());

            final SpringComponentModel springComponentModel = new SpringComponentModel();
            springComponentModel.setComponent(componentModel);
            springComponentModel.setType(firstSpringComponentModel.getType());
            springComponentModel.setObjectInstance(firstSpringComponentModel.getObjectInstance());
            springComponentModel.setBeanDefinition(firstSpringComponentModel.getBeanDefinition());
            springComponentModel.setBeanReference(firstSpringComponentModel.getBeanReference());
            return springComponentModel;
          } else {
            throw new IllegalStateException(format("Element %s does not have a building definition and it should since it's of type %s",
                                                   componentModel.getIdentifier(), wrapperElementType));
          }
        });
  }

  private BeanDefinitionCreator buildComponentModelProcessorChainOfResponsability() {
    EagerObjectCreator eagerObjectCreator = new EagerObjectCreator();
    ObjectBeanDefinitionCreator objectBeanDefinitionCreator = new ObjectBeanDefinitionCreator();
    PropertiesMapBeanDefinitionCreator propertiesMapBeanDefinitionCreator = new PropertiesMapBeanDefinitionCreator();
    ReferenceBeanDefinitionCreator referenceBeanDefinitionCreator = new ReferenceBeanDefinitionCreator();
    SimpleTypeBeanDefinitionCreator simpleTypeBeanDefinitionCreator = new SimpleTypeBeanDefinitionCreator();
    CollectionBeanDefinitionCreator collectionBeanDefinitionCreator = new CollectionBeanDefinitionCreator();
    MapEntryBeanDefinitionCreator mapEntryBeanDefinitionCreator = new MapEntryBeanDefinitionCreator();
    MapBeanDefinitionCreator mapBeanDefinitionCreator = new MapBeanDefinitionCreator();
    CommonBeanDefinitionCreator commonComponentModelProcessor = new CommonBeanDefinitionCreator(objectFactoryClassRepository);

    eagerObjectCreator.setNext(objectBeanDefinitionCreator);
    objectBeanDefinitionCreator.setNext(propertiesMapBeanDefinitionCreator);
    propertiesMapBeanDefinitionCreator.setNext(referenceBeanDefinitionCreator);
    referenceBeanDefinitionCreator.setNext(simpleTypeBeanDefinitionCreator);
    simpleTypeBeanDefinitionCreator.setNext(collectionBeanDefinitionCreator);
    collectionBeanDefinitionCreator.setNext(mapEntryBeanDefinitionCreator);
    mapEntryBeanDefinitionCreator.setNext(mapBeanDefinitionCreator);
    mapBeanDefinitionCreator.setNext(commonComponentModelProcessor);
    return eagerObjectCreator;
  }

  /**
   * Used to collaborate with the bean definition parsers mechanism.
   *
   * @param componentIdentifier a {@code ComponentModel} identifier.
   * @return true if there's a {@code ComponentBuildingDefinition} for the specified configuration identifier, false if there's
   * not.
   */
  public boolean hasDefinition(ComponentIdentifier componentIdentifier) {
    return isComponentIgnored(componentIdentifier)
        || customBuildersComponentIdentifiers.contains(componentIdentifier)
        || componentBuildingDefinitionRegistry.getBuildingDefinition(componentIdentifier).isPresent()
        || componentBuildingDefinitionRegistry.getWrappedComponent(componentIdentifier).isPresent();
  }

  /**
   * @param componentIdentifier the component identifier to check
   * @return {@code true} if the component identifier is one of the current language construct that have specific bean definitions parsers since we don't want to include
   * them in the parsing API.
   */
  public boolean isLanguageConstructComponent(ComponentIdentifier componentIdentifier) {
    return customBuildersComponentIdentifiers.contains(componentIdentifier);
  }

}
