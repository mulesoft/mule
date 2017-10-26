/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.beanutils.BeanUtils.copyProperty;
import static org.mule.runtime.api.component.Component.ANNOTATIONS_PROPERTY_NAME;
import static org.mule.runtime.config.internal.model.ApplicationModel.ANNOTATIONS_ELEMENT_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.CUSTOM_TRANSFORMER_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.MULE_PROPERTIES_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.MULE_PROPERTY_IDENTIFIER;
import static org.mule.runtime.config.internal.dsl.model.extension.xml.MacroExpansionModuleModel.ROOT_MACRO_EXPANDED_FLOW_CONTAINER_NAME;
import static org.mule.runtime.config.internal.dsl.processor.xml.XmlCustomAttributeHandler.from;
import static org.mule.runtime.config.internal.dsl.spring.BeanDefinitionFactory.SPRING_PROTOTYPE_OBJECT;
import static org.mule.runtime.config.internal.dsl.spring.PropertyComponentUtils.getPropertyValueFromPropertyComponent;
import static org.mule.runtime.core.privileged.execution.LocationExecutionContextProvider.addMetadataAnnotationsFromXml;
import static org.mule.runtime.deployment.model.internal.application.MuleApplicationClassLoader.resolveContextArtifactPluginClassLoaders;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;
import com.google.common.collect.ImmutableSet;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.config.internal.model.ComponentModel;
import org.mule.runtime.config.internal.dsl.model.SpringComponentModel;
import org.mule.runtime.config.internal.dsl.processor.ObjectTypeVisitor;
import org.mule.runtime.config.internal.dsl.processor.xml.XmlCustomAttributeHandler;
import org.mule.runtime.config.internal.factories.ModuleOperationMessageProcessorChainFactoryBean;
import org.mule.runtime.config.internal.parsers.XmlMetadataAnnotations;
import org.mule.runtime.config.privileged.dsl.BeanDefinitionPostProcessor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.registry.SpiServiceRegistry;
import org.mule.runtime.core.api.security.SecurityFilter;
import org.mule.runtime.core.privileged.processor.SecurityFilterMessageProcessor;
import org.mule.runtime.core.privileged.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Processor in the chain of responsibility that knows how to handle a generic {@code ComponentBuildingDefinition}.
 *
 * @since 4.0
 *        <p/>
 *        TODO MULE-9638 set visibility to package
 */
public class CommonBeanDefinitionCreator extends BeanDefinitionCreator {

  private static Set<ComponentIdentifier> genericPropertiesCustomProcessingIdentifiers =
      ImmutableSet.<ComponentIdentifier>builder()
          .add(CUSTOM_TRANSFORMER_IDENTIFIER)
          .build();

  private final ObjectFactoryClassRepository objectFactoryClassRepository;
  private BeanDefinitionPostProcessor beanDefinitionPostProcessor;

  public CommonBeanDefinitionCreator(ObjectFactoryClassRepository objectFactoryClassRepository) {
    this.objectFactoryClassRepository = objectFactoryClassRepository;

    this.beanDefinitionPostProcessor = resolvePostProcessor();
  }

  private BeanDefinitionPostProcessor resolvePostProcessor() {
    for (ClassLoader classLoader : resolveContextArtifactPluginClassLoaders()) {
      try {
        final BeanDefinitionPostProcessor foundProvider =
            new SpiServiceRegistry().lookupProvider(BeanDefinitionPostProcessor.class, classLoader);
        if (foundProvider != null) {
          return foundProvider;
        }
      } catch (Exception | ServiceConfigurationError e) {
        // Nothing to do, we just don't have compatibility plugin in the app
      }
    }
    return (componentModel, helper) -> {
    };
  }

  @Override
  public boolean handleRequest(CreateBeanDefinitionRequest request) {
    SpringComponentModel componentModel = request.getComponentModel();
    ComponentBuildingDefinition buildingDefinition = request.getComponentBuildingDefinition();
    componentModel.setType(retrieveComponentType(componentModel, buildingDefinition));
    BeanDefinitionBuilder beanDefinitionBuilder = createBeanDefinitionBuilder(componentModel, buildingDefinition);
    processAnnotations(componentModel, beanDefinitionBuilder);
    processComponentDefinitionModel(request.getParentComponentModel(), componentModel, buildingDefinition, beanDefinitionBuilder);
    return true;
  }

  private BeanDefinitionBuilder createBeanDefinitionBuilder(SpringComponentModel componentModel,
                                                            ComponentBuildingDefinition buildingDefinition) {
    BeanDefinitionBuilder beanDefinitionBuilder;
    if (buildingDefinition.getObjectFactoryType() != null) {
      beanDefinitionBuilder = createBeanDefinitionBuilderFromObjectFactory(componentModel, buildingDefinition);
    } else {
      beanDefinitionBuilder = genericBeanDefinition(componentModel.getType());
    }
    return beanDefinitionBuilder;
  }

  private void processAnnotationParameters(ComponentModel componentModel, Map<QName, Object> annotations) {
    componentModel.getParameters().entrySet().stream()
        .filter(entry -> entry.getKey().contains(":"))
        .forEach(annotationKey -> {
          Node attribute = from(componentModel).getNode().getAttributes().getNamedItem(annotationKey.getKey());
          if (attribute != null) {
            annotations.put(new QName(attribute.getNamespaceURI(), attribute.getLocalName()), annotationKey.getValue());
          }
        });
  }

  private void processNestedAnnotations(ComponentModel componentModel, Map<QName, Object> previousAnnotations) {
    componentModel.getInnerComponents().stream()
        .filter(cdm -> cdm.getIdentifier().equals(ANNOTATIONS_ELEMENT_IDENTIFIER))
        .findFirst()
        .ifPresent(annotationsCdm -> annotationsCdm.getInnerComponents().forEach(
                                                                                 annotationCdm -> previousAnnotations
                                                                                     .put(new QName(from(annotationCdm)
                                                                                         .getNamespaceUri(), annotationCdm
                                                                                             .getIdentifier()
                                                                                             .getName()),
                                                                                          annotationCdm.getTextContent())));
  }

  private void processAnnotations(ComponentModel componentModel, BeanDefinitionBuilder beanDefinitionBuilder) {
    if (Component.class.isAssignableFrom(componentModel.getType())) {
      XmlCustomAttributeHandler.ComponentCustomAttributeRetrieve customAttributeRetrieve = from(componentModel);
      Map<QName, Object> annotations =
          processMetadataAnnotationsHelper((Element) customAttributeRetrieve.getNode(), null, beanDefinitionBuilder);
      processAnnotationParameters(componentModel, annotations);
      processNestedAnnotations(componentModel, annotations);
      processMacroExpandedAnnotations(componentModel, annotations);
      if (!annotations.isEmpty()) {
        beanDefinitionBuilder.addPropertyValue(ANNOTATIONS_PROPERTY_NAME, annotations);
      }
    }
  }

  /**
   * Strictly needed when doing the macro expansion, if the current component model was not in the original <flow/> we need to
   * look for the rootest element that contains it (which happens to be the flow's name) so that later on from that name the
   * correct {@link ProcessingStrategy} will be picked up. Without that, all the streams managed by the runtime
   * ({@link CursorStreamProvider} won't be properly handled, ending up in closed streams or even deadlocks.
   * <p/>
   * Any alteration on this method should be tightly coupled with
   * {@link ModuleOperationMessageProcessorChainFactoryBean#doGetObject()}, which internally relies on
   * {@link DefaultMessageProcessorChainBuilder#newLazyProcessorChainBuilder(org.mule.runtime.core.privileged.processor.chain.AbstractMessageProcessorChainBuilder, org.mule.runtime.core.api.MuleContext, java.util.function.Supplier)}
   *
   * @param componentModel that might contain the <flow/>'s name attribute as a custom attribute
   * @param annotations to alter by adding the {@link AbstractComponent#ROOT_CONTAINER_NAME_KEY} if the component model has the
   *        name of the flow.
   */
  private void processMacroExpandedAnnotations(ComponentModel componentModel, Map<QName, Object> annotations) {
    if (componentModel.getCustomAttributes().containsKey(ROOT_MACRO_EXPANDED_FLOW_CONTAINER_NAME)) {
      final Object flowName = componentModel.getCustomAttributes().get(ROOT_MACRO_EXPANDED_FLOW_CONTAINER_NAME);
      annotations.put(AbstractComponent.ROOT_CONTAINER_NAME_KEY, flowName);
    }
  }

  private Map<QName, Object> processMetadataAnnotationsHelper(Element element, String configFileIdentifier,
                                                              BeanDefinitionBuilder builder) {
    Map<QName, Object> annotations = new HashMap<>();
    if (element == null) {
      return annotations;
    } else {
      if (Component.class.isAssignableFrom(builder.getBeanDefinition().getBeanClass())) {
        XmlMetadataAnnotations elementMetadata = (XmlMetadataAnnotations) element.getUserData("metadataAnnotations");
        addMetadataAnnotationsFromXml(annotations, elementMetadata.getElementString());
        builder.getBeanDefinition().getPropertyValues().addPropertyValue("annotations", annotations);
      }

      return annotations;
    }
  }



  private Class<?> retrieveComponentType(final ComponentModel componentModel,
                                         ComponentBuildingDefinition componentBuildingDefinition) {
    ObjectTypeVisitor objectTypeVisitor = new ObjectTypeVisitor(componentModel);
    componentBuildingDefinition.getTypeDefinition().visit(objectTypeVisitor);
    return objectTypeVisitor.getType();
  }

  private BeanDefinitionBuilder createBeanDefinitionBuilderFromObjectFactory(final SpringComponentModel componentModel,
                                                                             final ComponentBuildingDefinition componentBuildingDefinition) {
    ObjectTypeVisitor objectTypeVisitor = new ObjectTypeVisitor(componentModel);
    componentBuildingDefinition.getTypeDefinition().visit(objectTypeVisitor);
    Class<?> objectFactoryType = componentBuildingDefinition.getObjectFactoryType();

    Consumer<Object> instanceCustomizationFunction = object -> {
      Map<String, Object> customProperties = getTransformerCustomProperties(componentModel);
      if (!customProperties.isEmpty()) {
        injectSpringProperties(customProperties, object);
      }
    };

    return rootBeanDefinition(objectFactoryClassRepository
        .getObjectFactoryClass(componentBuildingDefinition, objectFactoryType, objectTypeVisitor.getType(),
                               () -> componentModel.getBeanDefinition().isLazyInit(), instanceCustomizationFunction));
  }

  private void injectSpringProperties(Map<String, Object> customProperties, Object createdInstance) {
    try {
      for (String propertyName : customProperties.keySet()) {
        copyProperty(createdInstance, propertyName, customProperties.get(propertyName));
      }
    } catch (Exception e) {
      throw new MuleRuntimeException(e);
    }
  }

  private Map<String, Object> getTransformerCustomProperties(ComponentModel componentModel) {
    ComponentIdentifier identifier = componentModel.getIdentifier();
    if (!identifier.equals(CUSTOM_TRANSFORMER_IDENTIFIER)) {
      return emptyMap();
    }
    return componentModel.getInnerComponents().stream()
        .filter(innerComponent -> innerComponent.getIdentifier().equals(MULE_PROPERTY_IDENTIFIER))
        .map(springComponent -> getPropertyValueFromPropertyComponent(springComponent))
        .collect(toMap(propValue -> propValue.getFirst(), propValue -> propValue.getSecond()));
  }

  private void processComponentDefinitionModel(final ComponentModel parentComponentModel,
                                               final SpringComponentModel componentModel,
                                               ComponentBuildingDefinition componentBuildingDefinition,
                                               final BeanDefinitionBuilder beanDefinitionBuilder) {
    processObjectConstructionParameters(componentModel, componentBuildingDefinition,
                                        new BeanDefinitionBuilderHelper(beanDefinitionBuilder));
    processMuleProperties(componentModel, beanDefinitionBuilder, beanDefinitionPostProcessor);
    if (componentBuildingDefinition.isPrototype()) {
      beanDefinitionBuilder.setScope(SPRING_PROTOTYPE_OBJECT);
    }
    AbstractBeanDefinition originalBeanDefinition = beanDefinitionBuilder.getBeanDefinition();
    AbstractBeanDefinition wrappedBeanDefinition = adaptBeanDefinition(parentComponentModel, originalBeanDefinition);
    if (originalBeanDefinition != wrappedBeanDefinition) {
      componentModel.setType(wrappedBeanDefinition.getBeanClass());
    }
    final SpringPostProcessorIocHelper iocHelper =
        new SpringPostProcessorIocHelper(objectFactoryClassRepository, wrappedBeanDefinition);
    beanDefinitionPostProcessor.postProcess(componentModel.getConfiguration(), iocHelper);
    componentModel.setBeanDefinition(iocHelper.getBeanDefinition());
  }

  static void processMuleProperties(ComponentModel componentModel, BeanDefinitionBuilder beanDefinitionBuilder,
                                    BeanDefinitionPostProcessor beanDefinitionPostProcessor) {
    // for now we skip custom-transformer since requires injection by the object factory.
    if (genericPropertiesCustomProcessingIdentifiers.contains(componentModel.getIdentifier())
        || (beanDefinitionPostProcessor != null && beanDefinitionPostProcessor.getGenericPropertiesCustomProcessingIdentifiers()
            .contains(componentModel.getIdentifier()))) {
      return;
    }
    componentModel.getInnerComponents()
        .stream()
        .filter(innerComponent -> {
          ComponentIdentifier identifier = innerComponent.getIdentifier();
          return identifier.equals(MULE_PROPERTY_IDENTIFIER)
              || identifier.equals(MULE_PROPERTIES_IDENTIFIER);
        })
        .forEach(propertyComponentModel -> {
          Pair<String, Object> propertyValue = getPropertyValueFromPropertyComponent(propertyComponentModel);
          beanDefinitionBuilder.addPropertyValue(propertyValue.getFirst(), propertyValue.getSecond());
        });
  }

  public static List<Pair<String, Object>> getPropertyValueFromPropertiesComponent(ComponentModel propertyComponentModel) {
    List<Pair<String, Object>> propertyValues = new ArrayList<>();
    propertyComponentModel.getInnerComponents().stream().forEach(entryComponentModel -> {
      propertyValues.add(new Pair<>(entryComponentModel.getParameters().get("key"),
                                    entryComponentModel.getParameters().get("value")));
    });
    return propertyValues;
  }

  private void processObjectConstructionParameters(final ComponentModel componentModel,
                                                   final ComponentBuildingDefinition componentBuildingDefinition,
                                                   final BeanDefinitionBuilderHelper beanDefinitionBuilderHelper) {
    new ComponentConfigurationBuilder(componentModel, componentBuildingDefinition, beanDefinitionBuilderHelper)
        .processConfiguration();

  }

  private AbstractBeanDefinition adaptBeanDefinition(ComponentModel parentComponentModel,
                                                     AbstractBeanDefinition originalBeanDefinition) {
    Class beanClass;
    if (originalBeanDefinition instanceof RootBeanDefinition) {
      beanClass = ((RootBeanDefinition) originalBeanDefinition).getBeanClass();
    } else {
      try {
        beanClass = originalBeanDefinition.getBeanClass();
      } catch (IllegalStateException e) {
        try {
          beanClass = org.apache.commons.lang3.ClassUtils.getClass(originalBeanDefinition.getBeanClassName());
        } catch (ClassNotFoundException e2) {
          throw new RuntimeException(e2);
        }
      }
    }

    BeanDefinition newBeanDefinition;
    if (areMatchingTypes(SecurityFilter.class, beanClass)) {
      newBeanDefinition = rootBeanDefinition(SecurityFilterMessageProcessor.class)
          .addConstructorArgValue(originalBeanDefinition)
          .getBeanDefinition();
      return (AbstractBeanDefinition) newBeanDefinition;
    } else {
      final SpringPostProcessorIocHelper iocHelper =
          new SpringPostProcessorIocHelper(objectFactoryClassRepository, originalBeanDefinition);
      beanDefinitionPostProcessor.adaptBeanDefinition(parentComponentModel.getConfiguration(), beanClass, iocHelper);
      return iocHelper.getBeanDefinition();
    }
  }

  public static boolean areMatchingTypes(Class<?> superType, Class<?> childType) {
    return superType.isAssignableFrom(childType);
  }

}
