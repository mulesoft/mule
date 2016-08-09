/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.spring;

import static org.mule.runtime.api.meta.AnnotatedObject.PROPERTY_NAME;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.ANNOTATIONS_ELEMENT_IDENTIFIER;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.DEFAULT_ES_ELEMENT_IDENTIFIER;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.FILTER_ELEMENT_SUFFIX;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.MESSAGE_FILTER_ELEMENT_IDENTIFIER;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.MULE_IDENTIFIER;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.MULE_PROPERTY_IDENTIFIER;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.SPRING_PROPERTY_IDENTIFIER;
import static org.mule.runtime.config.spring.dsl.processor.xml.XmlCustomAttributeHandler.from;
import static org.mule.runtime.config.spring.dsl.spring.PropertyComponentUtils.getPropertyValueFromPropertyComponent;
import static org.mule.runtime.config.spring.parsers.AbstractMuleBeanDefinitionParser.processMetadataAnnotationsHelper;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;
import org.mule.runtime.api.meta.AnnotatedObject;
import org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinition;
import org.mule.runtime.config.spring.dsl.model.ComponentIdentifier;
import org.mule.runtime.config.spring.dsl.model.ComponentModel;
import org.mule.runtime.config.spring.dsl.processor.ObjectTypeVisitor;
import org.mule.runtime.config.spring.dsl.processor.xml.XmlCustomAttributeHandler;
import org.mule.runtime.core.api.routing.filter.Filter;
import org.mule.runtime.core.api.security.SecurityFilter;
import org.mule.runtime.core.processor.SecurityFilterMessageProcessor;
import org.mule.runtime.core.routing.MessageFilter;
import org.mule.runtime.core.util.ClassUtils;

import com.google.common.collect.ImmutableSet;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.cglib.proxy.Callback;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Processor in the chain of responsibility that knows how to handle a generic {@code ComponentBuildingDefinition}.
 *
 * @since 4.0
 *        <p/>
 *        TODO MULE-9638 set visibility to package
 */
public class CommonBeanDefinitionCreator extends BeanDefinitionCreator {

  private static final String SPRING_PROTOTYPE_OBJECT = "prototype";
  private static final String TRANSPORT_BEAN_DEFINITION_POST_PROCESSOR_CLASS =
      "org.mule.compatibility.config.spring.parsers.specific.TransportElementBeanDefinitionPostProcessor";
  private static final ImmutableSet<ComponentIdentifier> MESSAGE_FILTER_WRAPPERS = new ImmutableSet.Builder<ComponentIdentifier>()
      .add(MESSAGE_FILTER_ELEMENT_IDENTIFIER).add(MULE_IDENTIFIER).add(DEFAULT_ES_ELEMENT_IDENTIFIER).build();

  private BeanDefinitionPostProcessor beanDefinitionPostProcessor;

  public CommonBeanDefinitionCreator() {
    try {
      // TODO MULE-9728 - Provide a mechanism to hook per transport in the endpoint address parsing
      this.beanDefinitionPostProcessor = (BeanDefinitionPostProcessor) ClassUtils
          .getClass(Thread.currentThread().getContextClassLoader(), TRANSPORT_BEAN_DEFINITION_POST_PROCESSOR_CLASS).newInstance();
    } catch (Exception e) {
      this.beanDefinitionPostProcessor = (componentModel, beanDefinition) -> {
      };
    }
  }

  @Override
  public boolean handleRequest(CreateBeanDefinitionRequest request) {
    ComponentModel componentModel = request.getComponentModel();
    ComponentBuildingDefinition buildingDefinition = request.getComponentBuildingDefinition();
    componentModel.setType(retrieveComponentType(componentModel, buildingDefinition));
    BeanDefinitionBuilder beanDefinitionBuilder = createBeanDefinitionBuilder(componentModel, buildingDefinition);
    processAnnotations(componentModel, beanDefinitionBuilder);
    processComponentDefinitionModel(request.getParentComponentModel(), componentModel, buildingDefinition, beanDefinitionBuilder);
    return true;
  }

  private BeanDefinitionBuilder createBeanDefinitionBuilder(ComponentModel componentModel,
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
    componentModel.getParameters().entrySet().stream().filter(entry -> entry.getKey().contains(":")).forEach(annotationKey -> {
      Node attribute = from(componentModel).getNode().getAttributes().getNamedItem(annotationKey.getKey());
      if (attribute != null) {
        annotations.put(new QName(attribute.getNamespaceURI(), attribute.getLocalName()), annotationKey.getValue());
      }
    });
  }

  private void processNestedAnnotations(ComponentModel componentModel, Map<QName, Object> previousAnnotations) {
    componentModel.getInnerComponents().stream().filter(cdm -> cdm.getIdentifier().equals(ANNOTATIONS_ELEMENT_IDENTIFIER))
        .findFirst()
        .ifPresent(annotationsCdm -> annotationsCdm.getInnerComponents()
            .forEach(annotationCdm -> previousAnnotations
                .put(new QName(from(annotationCdm).getNamespaceUri(), annotationCdm.getIdentifier().getName()),
                     annotationCdm.getTextContent())));
  }

  private void processAnnotations(ComponentModel componentModel, BeanDefinitionBuilder beanDefinitionBuilder) {
    if (AnnotatedObject.class.isAssignableFrom(componentModel.getType())) {
      XmlCustomAttributeHandler.ComponentCustomAttributeRetrieve customAttributeRetrieve = from(componentModel);
      Map<QName, Object> annotations =
          processMetadataAnnotationsHelper((Element) customAttributeRetrieve.getNode(),
                                           customAttributeRetrieve.getConfigFileName(), beanDefinitionBuilder);
      processAnnotationParameters(componentModel, annotations);
      processNestedAnnotations(componentModel, annotations);
      if (!annotations.isEmpty()) {
        beanDefinitionBuilder.addPropertyValue(PROPERTY_NAME, annotations);
      }
    }
  }


  private Class<?> retrieveComponentType(final ComponentModel componentModel,
                                         ComponentBuildingDefinition componentBuildingDefinition) {
    ObjectTypeVisitor objectTypeVisitor = new ObjectTypeVisitor(componentModel);
    componentBuildingDefinition.getTypeDefinition().visit(objectTypeVisitor);
    return objectTypeVisitor.getType();
  }

  private BeanDefinitionBuilder createBeanDefinitionBuilderFromObjectFactory(final ComponentModel componentModel,
                                                                             final ComponentBuildingDefinition componentBuildingDefinition) {
    /*
     * We need this to allow spring create the object using a FactoryBean but using the object factory setters and getters so we
     * create as FactoryBean a dynamic class that will have the same attributes and methods as the ObjectFactory that the user
     * defined. This way our API does not expose spring specific classes.
     */
    ObjectTypeVisitor objectTypeVisitor = new ObjectTypeVisitor(componentModel);
    componentBuildingDefinition.getTypeDefinition().visit(objectTypeVisitor);
    BeanDefinitionBuilder beanDefinitionBuilder;
    Class<?> objectFactoryType = componentBuildingDefinition.getObjectFactoryType();
    Enhancer enhancer = new Enhancer();
    enhancer.setInterfaces(new Class[] {FactoryBean.class});
    enhancer.setSuperclass(objectFactoryType);
    enhancer.setCallbackType(MethodInterceptor.class);
    Class factoryBeanClass = enhancer.createClass();
    Enhancer.registerStaticCallbacks(factoryBeanClass, new Callback[] {new MethodInterceptor() {

      @Override
      public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        if (method.getName().equals("isSingleton")) {
          return !componentBuildingDefinition.isPrototype();
        }
        if (method.getName().equals("getObjectType")) {
          return objectTypeVisitor.getType();
        }
        return proxy.invokeSuper(obj, args);
      }
    }});
    beanDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(factoryBeanClass);
    return beanDefinitionBuilder;
  }

  private void processComponentDefinitionModel(final ComponentModel parentComponentModel, final ComponentModel componentModel,
                                               ComponentBuildingDefinition componentBuildingDefinition,
                                               final BeanDefinitionBuilder beanDefinitionBuilder) {
    processObjectConstructionParameters(componentModel, componentBuildingDefinition,
                                        new BeanDefinitionBuilderHelper(beanDefinitionBuilder));
    processSpringOrMuleProperties(componentModel, beanDefinitionBuilder);
    if (componentBuildingDefinition.isPrototype()) {
      beanDefinitionBuilder.setScope(SPRING_PROTOTYPE_OBJECT);
    }
    AbstractBeanDefinition originalBeanDefinition = beanDefinitionBuilder.getBeanDefinition();
    AbstractBeanDefinition wrappedBeanDefinition = adaptFilterBeanDefinitions(parentComponentModel, originalBeanDefinition);
    if (originalBeanDefinition != wrappedBeanDefinition) {
      componentModel.setType(wrappedBeanDefinition.getBeanClass());
    }
    beanDefinitionPostProcessor.postProcess(componentModel, wrappedBeanDefinition);
    componentModel.setBeanDefinition(wrappedBeanDefinition);
  }

  // TODO MULE-9638 Remove once we don't mix spring beans with mule beans.
  private void processSpringOrMuleProperties(ComponentModel componentModel, BeanDefinitionBuilder beanDefinitionBuilder) {
    componentModel.getInnerComponents().stream().filter(innerComponent -> {
      ComponentIdentifier identifier = innerComponent.getIdentifier();
      return identifier.equals(SPRING_PROPERTY_IDENTIFIER) || identifier.equals(MULE_PROPERTY_IDENTIFIER);
    }).forEach(propertyComponentModel -> {
      PropertyValue propertyValue = getPropertyValueFromPropertyComponent(propertyComponentModel);
      beanDefinitionBuilder.addPropertyValue(propertyValue.getName(), propertyValue.getValue());
    });
  }

  public static List<PropertyValue> getPropertyValueFromPropertiesComponent(ComponentModel propertyComponentModel) {
    List<PropertyValue> propertyValues = new ArrayList<>();
    propertyComponentModel.getInnerComponents().stream().forEach(entryComponentModel -> {
      propertyValues.add(new PropertyValue(entryComponentModel.getParameters().get("key"),
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

  public static AbstractBeanDefinition adaptFilterBeanDefinitions(ComponentModel parentComponentModel,
                                                                  AbstractBeanDefinition originalBeanDefinition) {
    // TODO this condition may be removed
    if (originalBeanDefinition == null) {
      return null;
    }
    if (!filterWrapperRequired(parentComponentModel)) {
      return originalBeanDefinition;
    }
    Class beanClass;
    if (originalBeanDefinition instanceof RootBeanDefinition) {
      beanClass = ((RootBeanDefinition) originalBeanDefinition).getBeanClass();
    } else {
      // TODO see if this condition can be removed.
      if (originalBeanDefinition.getBeanClassName() == null) {
        return originalBeanDefinition;
      }
      try {
        beanClass = ClassUtils.getClass(originalBeanDefinition.getBeanClassName());
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    }
    BeanDefinition newBeanDefinition;
    if (areMatchingTypes(Filter.class, beanClass)) {
      boolean failOnUnaccepted = false;
      Object processorWhenUnaccepted = null;
      newBeanDefinition =
          BeanDefinitionBuilder.rootBeanDefinition(MessageFilter.class).addConstructorArgValue(originalBeanDefinition)
              .addConstructorArgValue(failOnUnaccepted).addConstructorArgValue(processorWhenUnaccepted).getBeanDefinition();
      return (AbstractBeanDefinition) newBeanDefinition;
    } else if (areMatchingTypes(SecurityFilter.class, beanClass)) {
      newBeanDefinition = BeanDefinitionBuilder.rootBeanDefinition(SecurityFilterMessageProcessor.class)
          .addPropertyValue("filter", originalBeanDefinition).getBeanDefinition();
      return (AbstractBeanDefinition) newBeanDefinition;
    }
    return originalBeanDefinition;
  }

  private static boolean filterWrapperRequired(ComponentModel parentComponentModel) {
    return !MESSAGE_FILTER_WRAPPERS.contains(parentComponentModel.getIdentifier())
        && !parentComponentModel.getIdentifier().getName().endsWith(FILTER_ELEMENT_SUFFIX);
  }

  public static boolean areMatchingTypes(Class<?> superType, Class<?> childType) {
    return superType.isAssignableFrom(childType);
  }

  public interface BeanDefinitionPostProcessor {

    void postProcess(ComponentModel componentModel, AbstractBeanDefinition beanDefinition);
  }

}
