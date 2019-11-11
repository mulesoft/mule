/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.component.AbstractComponent.LOCATION_KEY;
import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.serialization.ObjectSerializer.DEFAULT_OBJECT_SERIALIZER_NAME;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.CONFIGURATION_IDENTIFIER;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.MULE_DOMAIN_IDENTIFIER;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.MULE_EE_DOMAIN_IDENTIFIER;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.MULE_IDENTIFIER;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.RAISE_ERROR_IDENTIFIER;
import static org.mule.runtime.config.api.dsl.model.ComponentBuildingDefinitionRegistry.WrapperElementType.SINGLE;
import static org.mule.runtime.config.internal.dsl.spring.CommonBeanDefinitionCreator.areMatchingTypes;
import static org.mule.runtime.config.internal.dsl.spring.ComponentModelHelper.addAnnotation;
import static org.mule.runtime.config.internal.model.ApplicationModel.ANNOTATIONS_ELEMENT_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.DESCRIPTION_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.DOC_DESCRIPTION_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.ERROR_MAPPING_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.GLOBAL_PROPERTY_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.MULE_PROPERTIES_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.MULE_PROPERTY_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.OBJECT_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.SECURITY_MANAGER_IDENTIFIER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_EXPRESSION_LANGUAGE;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.ANY;
import static org.mule.runtime.core.internal.component.ComponentAnnotations.ANNOTATION_COMPONENT_CONFIG;
import static org.mule.runtime.core.internal.component.ComponentAnnotations.ANNOTATION_NAME;
import static org.mule.runtime.core.internal.component.ComponentAnnotations.ANNOTATION_PARAMETERS;
import static org.mule.runtime.core.internal.exception.ErrorMapping.ANNOTATION_ERROR_MAPPINGS;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.functional.Either;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.config.api.dsl.model.ComponentBuildingDefinitionRegistry;
import org.mule.runtime.config.api.dsl.model.properties.ConfigurationPropertiesProviderFactory;
import org.mule.runtime.config.internal.SpringConfigurationComponentLocator;
import org.mule.runtime.config.internal.dsl.model.SpringComponentModel;
import org.mule.runtime.config.internal.model.ComponentModel;
import org.mule.runtime.core.api.exception.ErrorTypeMatcher;
import org.mule.runtime.core.api.exception.SingleErrorTypeMatcher;
import org.mule.runtime.core.internal.el.mvel.MVELExpressionLanguage;
import org.mule.runtime.core.internal.exception.ErrorMapping;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.w3c.dom.Element;

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
          .add(MULE_IDENTIFIER)
          .add(MULE_DOMAIN_IDENTIFIER)
          .add(MULE_EE_DOMAIN_IDENTIFIER)
          .add(ERROR_MAPPING_IDENTIFIER)
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


  private final ComponentBuildingDefinitionRegistry componentBuildingDefinitionRegistry;
  private final BeanDefinitionCreator componentModelProcessor;
  private final ErrorTypeRepository errorTypeRepository;
  private final ObjectFactoryClassRepository objectFactoryClassRepository = new ObjectFactoryClassRepository();
  private final Set<String> syntheticErrorNamespaces = new HashSet<>();

  /**
   * @param componentBuildingDefinitionRegistry a registry with all the known {@code ComponentBuildingDefinition}s by the
   *        artifact.
   * @param errorTypeRepository
   */
  public BeanDefinitionFactory(ComponentBuildingDefinitionRegistry componentBuildingDefinitionRegistry,
                               ErrorTypeRepository errorTypeRepository) {
    this.componentBuildingDefinitionRegistry = componentBuildingDefinitionRegistry;
    this.errorTypeRepository = errorTypeRepository;
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
   * Creates a {@code BeanDefinition} by traversing the {@code ComponentModel} and its children.
   *
   * @param componentModel the component model from which we want to create the bean definition.
   * @param registry the bean registry since it may be required to get other bean definitions to create this one or to register
   *        the bean definition.
   * @param componentModelPostProcessor a function to post process the bean definition.
   * @param oldParsingMechanism a function to execute the old parsing mechanism if required by children {@code ComponentModel}s
   * @param componentLocator where the locations of any {@link Component}'s locations must be registered
   * @return the {@code BeanDefinition} of the component model.
   */
  public BeanDefinition resolveComponentRecursively(SpringComponentModel componentModel, BeanDefinitionRegistry registry,
                                                    BiConsumer<ComponentModel, BeanDefinitionRegistry> componentModelPostProcessor,
                                                    BiFunction<Element, BeanDefinition, Either<BeanDefinition, BeanReference>> oldParsingMechanism,
                                                    SpringConfigurationComponentLocator componentLocator) {
    List<ComponentModel> innerComponents = componentModel.getInnerComponents();
    if (!innerComponents.isEmpty()) {
      for (ComponentModel innerComponent : innerComponents) {
        resolveComponentRecursively((SpringComponentModel) innerComponent, registry,
                                    componentModelPostProcessor, oldParsingMechanism, componentLocator);
      }
    }
    return resolveComponent(componentModel, registry, componentModelPostProcessor, componentLocator);
  }

  private BeanDefinition resolveComponent(SpringComponentModel componentModel,
                                          BeanDefinitionRegistry registry,
                                          BiConsumer<ComponentModel, BeanDefinitionRegistry> componentDefinitionModelProcessor,
                                          SpringConfigurationComponentLocator componentLocator) {
    if (isComponentIgnored(componentModel.getIdentifier())) {
      return null;
    }

    resolveComponentBeanDefinition(componentModel);
    componentDefinitionModelProcessor.accept(componentModel, registry);

    // TODO MULE-9638: Once we migrate all core definitions we need to define a mechanism for customizing
    // how core constructs are processed.
    processMuleConfiguration(componentModel, registry);
    processMuleSecurityManager(componentModel, registry);
    processRaiseError(componentModel);

    componentBuildingDefinitionRegistry.getBuildingDefinition(componentModel.getIdentifier())
        .ifPresent(componentBuildingDefinition -> {
          if ((componentModel.getType() != null) && Component.class.isAssignableFrom(componentModel.getType())) {
            addAnnotation(ANNOTATION_NAME, componentModel.getIdentifier(), componentModel);
            // We need to use a mutable map since spring will resolve the properties placeholder present in the value if needed
            // and it will be done by mutating the same map.
            addAnnotation(ANNOTATION_PARAMETERS, new HashMap<>(componentModel.getParameters()), componentModel);
            // add any error mappings if present
            List<ComponentModel> errorMappingComponents = componentModel.getInnerComponents().stream()
                .filter(innerComponent -> ERROR_MAPPING_IDENTIFIER.equals(innerComponent.getIdentifier())).collect(toList());
            if (!errorMappingComponents.isEmpty()) {
              addAnnotation(ANNOTATION_ERROR_MAPPINGS, errorMappingComponents.stream().map(innerComponent -> {
                Map<String, String> parameters = innerComponent.getParameters();
                ComponentIdentifier source = parameters.containsKey(SOURCE_TYPE)
                    ? buildFromStringRepresentation(parameters.get(SOURCE_TYPE))
                    : ANY;

                ErrorType errorType = errorTypeRepository
                    .lookupErrorType(source)
                    .orElseThrow(() -> new MuleRuntimeException(createStaticMessage("Could not find error '%s'.", source)));

                ErrorTypeMatcher errorTypeMatcher = new SingleErrorTypeMatcher(errorType);
                ErrorType targetValue = resolveErrorType(parameters.get(TARGET_TYPE));
                return new ErrorMapping(errorTypeMatcher, targetValue);
              }).collect(toList()), componentModel);
            }
            componentLocator.addComponentLocation(componentModel.getComponentLocation());
            addAnnotation(ANNOTATION_COMPONENT_CONFIG, componentModel, componentModel);
          }
        });

    addAnnotation(LOCATION_KEY, componentModel.getComponentLocation(), componentModel);

    BeanDefinition beanDefinition = componentModel.getBeanDefinition();
    return beanDefinition;
  }

  private void processRaiseError(ComponentModel componentModel) {
    if (componentModel.getIdentifier().equals(RAISE_ERROR_IDENTIFIER)) {
      resolveErrorType(componentModel.getParameters().get("type"));
    }
  }

  private ErrorType resolveErrorType(String representation) {
    int separator = representation.indexOf(":");
    String namespace;
    String identifier;
    if (separator > 0) {
      namespace = representation.substring(0, separator).toUpperCase();
      identifier = representation.substring(separator + 1).toUpperCase();
    } else {
      namespace = CORE_ERROR_NS;
      identifier = representation.toUpperCase();
    }

    ComponentIdentifier errorIdentifier = ComponentIdentifier.builder().namespace(namespace).name(identifier).build();
    if (CORE_ERROR_NS.equals(namespace)) {
      return errorTypeRepository.lookupErrorType(errorIdentifier)
          .orElseThrow(() -> new MuleRuntimeException(createStaticMessage(format("There's no MULE error named '%s'.",
                                                                                 identifier))));
    } else if (errorTypeRepository.getErrorNamespaces().contains(namespace) && !syntheticErrorNamespaces.contains(namespace)) {
      throw new MuleRuntimeException(createStaticMessage(format("Cannot use error type '%s:%s': namespace already exists.",
                                                                namespace, identifier)));
    } else if (syntheticErrorNamespaces.contains(namespace)) {
      Optional<ErrorType> optionalErrorType = errorTypeRepository.lookupErrorType(errorIdentifier);
      if (optionalErrorType.isPresent()) {
        return optionalErrorType.get();
      }
    } else {
      syntheticErrorNamespaces.add(namespace);
    }
    return errorTypeRepository.addErrorType(errorIdentifier, errorTypeRepository.getAnyErrorType());
  }

  private void processMuleConfiguration(ComponentModel componentModel, BeanDefinitionRegistry registry) {
    if (componentModel.getIdentifier().equals(CONFIGURATION_IDENTIFIER)) {
      AtomicReference<BeanDefinition> expressionLanguage = new AtomicReference<>();

      componentModel.getInnerComponents().stream().forEach(childComponentModel -> {
        if (areMatchingTypes(MVELExpressionLanguage.class, childComponentModel.getType())) {
          expressionLanguage.set(((SpringComponentModel) childComponentModel).getBeanDefinition());
        }
      });
      String defaultObjectSerializer = componentModel.getParameters().get("defaultObjectSerializer-ref");
      if (defaultObjectSerializer != null) {
        if (defaultObjectSerializer != DEFAULT_OBJECT_SERIALIZER_NAME) {
          registry.removeBeanDefinition(DEFAULT_OBJECT_SERIALIZER_NAME);
          registry.registerAlias(defaultObjectSerializer, DEFAULT_OBJECT_SERIALIZER_NAME);
        }
      }
      if (expressionLanguage.get() != null) {
        registry.registerBeanDefinition(OBJECT_EXPRESSION_LANGUAGE, expressionLanguage.get());
      }
    }
  }

  private void processMuleSecurityManager(ComponentModel componentModel, BeanDefinitionRegistry registry) {
    if (componentModel.getIdentifier().equals(SECURITY_MANAGER_IDENTIFIER)) {
      componentModel.getInnerComponents().stream().forEach(childComponentModel -> {
        String identifier = childComponentModel.getIdentifier().getName();
        if (identifier.equals("password-encryption-strategy")
            || identifier.equals("secret-key-encryption-strategy")) {
          registry.registerBeanDefinition(childComponentModel.getNameAttribute(),
                                          ((SpringComponentModel) childComponentModel).getBeanDefinition());
        }
      });
    }
  }


  private void resolveComponentBeanDefinition(SpringComponentModel componentModel) {
    Optional<ComponentBuildingDefinition<?>> buildingDefinitionOptional =
        componentBuildingDefinitionRegistry.getBuildingDefinition(componentModel.getIdentifier());
    if (buildingDefinitionOptional.isPresent() || customBuildersComponentIdentifiers.contains(componentModel.getIdentifier())) {
      this.componentModelProcessor.processRequest(new CreateBeanDefinitionRequest(componentModel,
                                                                                  buildingDefinitionOptional.orElse(null)));
    } else {
      processComponentWrapper(componentModel);
    }
  }

  private void processComponentWrapper(SpringComponentModel componentModel) {
    componentBuildingDefinitionRegistry.getWrappedComponent(componentModel.getIdentifier()).ifPresent(wrapperElementType -> {
      if (wrapperElementType.equals(SINGLE)) {
        if (componentModel.getInnerComponents().isEmpty()) {
          String location =
              componentModel.getComponentLocation() != null ? componentModel.getComponentLocation().getLocation() : "";
          throw new IllegalStateException(format("Element [%s] located at [%s] does not have any child element declared, but one is required.",
                                                 componentModel.getIdentifier(), location));
        }
        final SpringComponentModel firstComponentModel = (SpringComponentModel) componentModel.getInnerComponents().get(0);
        componentModel.setType(firstComponentModel.getType());
        componentModel.setBeanDefinition(firstComponentModel.getBeanDefinition());
        componentModel.setBeanReference(firstComponentModel.getBeanReference());
      } else {
        throw new IllegalStateException(format("Element %s does not have a building definition and it should since it's of type %s",
                                               componentModel.getIdentifier(), wrapperElementType));
      }
    });
  }

  private BeanDefinitionCreator buildComponentModelProcessorChainOfResponsability() {
    EagerObjectCreator eagerObjectCreator = new EagerObjectCreator();
    ObjectBeanDefinitionCreator objectBeanDefinitionCreator = new ObjectBeanDefinitionCreator();
    ExceptionStrategyRefBeanDefinitionCreator exceptionStrategyRefBeanDefinitionCreator =
        new ExceptionStrategyRefBeanDefinitionCreator();
    PropertiesMapBeanDefinitionCreator propertiesMapBeanDefinitionCreator = new PropertiesMapBeanDefinitionCreator();
    ReferenceBeanDefinitionCreator referenceBeanDefinitionCreator = new ReferenceBeanDefinitionCreator();
    SimpleTypeBeanDefinitionCreator simpleTypeBeanDefinitionCreator = new SimpleTypeBeanDefinitionCreator();
    CollectionBeanDefinitionCreator collectionBeanDefinitionCreator = new CollectionBeanDefinitionCreator();
    MapEntryBeanDefinitionCreator mapEntryBeanDefinitionCreator = new MapEntryBeanDefinitionCreator();
    MapBeanDefinitionCreator mapBeanDefinitionCreator = new MapBeanDefinitionCreator();
    CommonBeanDefinitionCreator commonComponentModelProcessor = new CommonBeanDefinitionCreator(objectFactoryClassRepository);

    eagerObjectCreator.setNext(objectBeanDefinitionCreator);
    objectBeanDefinitionCreator.setNext(propertiesMapBeanDefinitionCreator);
    propertiesMapBeanDefinitionCreator.setNext(exceptionStrategyRefBeanDefinitionCreator);
    exceptionStrategyRefBeanDefinitionCreator.setNext(referenceBeanDefinitionCreator);
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
   *         not.
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
