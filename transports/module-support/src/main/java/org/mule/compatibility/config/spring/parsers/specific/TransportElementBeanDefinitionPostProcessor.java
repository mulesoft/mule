/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.config.spring.parsers.specific;

import static org.mule.compatibility.config.spring.TransportComponentBuildingDefinitionProvider.ENDPOINT_ELEMENT;
import static org.mule.compatibility.config.spring.TransportComponentBuildingDefinitionProvider.INBOUND_ENDPOINT_ELEMENT;
import static org.mule.compatibility.config.spring.TransportComponentBuildingDefinitionProvider.OUTBOUND_ENDPOINT_ELEMENT;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.MULE_PROPERTY_IDENTIFIER;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.SPRING_PROPERTY_IDENTIFIER;
import static org.mule.runtime.config.spring.dsl.spring.CommonBeanDefinitionCreator.areMatchingTypes;
import static org.mule.runtime.config.spring.dsl.spring.PropertyComponentUtils.getPropertyValueFromPropertyComponent;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_CONTEXT;

import org.mule.compatibility.core.endpoint.URIBuilder;
import org.mule.runtime.core.config.ComponentIdentifier;
import org.mule.runtime.config.spring.dsl.model.ComponentModel;
import org.mule.runtime.config.spring.dsl.spring.CommonBeanDefinitionCreator;
import org.mule.runtime.config.spring.factories.MessageProcessorChainFactoryBean;
import org.mule.runtime.core.processor.AbstractRedeliveryPolicy;
import org.mule.runtime.core.util.StringUtils;

import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;

/**
 * This is a post processor that will run for every {@link ComponentModel} parsing. In case that the {@code ComponentModel} is an
 * endpoint then this class will do specific parsing for endpoints by manipulating the {@code BeanDefinition}.
 *
 * The complex logic to parse endpoints is not required anymore and we have created this class to avoid polluting the new parsing
 * mechanism with transport specific logic.
 *
 * @since 4.0
 */
public class TransportElementBeanDefinitionPostProcessor implements CommonBeanDefinitionCreator.BeanDefinitionPostProcessor {

  private static final String PROPERTIES_ELEMENT = "properties";
  private static final String TRANSFORMERS_SEPARATOR = " ";
  private static final String REFERENCE_ATTRIBUTE = "ref";

  @Override
  public void postProcess(ComponentModel componentModel, AbstractBeanDefinition modelBeanDefinition) {
    if (componentModel.getIdentifier().getName().equals(INBOUND_ENDPOINT_ELEMENT)
        || componentModel.getIdentifier().getName().equals(OUTBOUND_ENDPOINT_ELEMENT)
        || componentModel.getIdentifier().getName().equals(ENDPOINT_ELEMENT)) {
      processReferenceParameter(componentModel, modelBeanDefinition);
      processAddressParameter(componentModel, modelBeanDefinition);
      processTransformerReferences(componentModel, modelBeanDefinition);
      ManagedList messageProcessors = processMessageProcessors(componentModel, modelBeanDefinition);
      // TODO MULE-9728 - Provide a mechanism to hook per transport in the endpoint address parsing
      processRedeliveryPolicy(modelBeanDefinition, messageProcessors);
    }
    if (componentModel.getIdentifier().getName().endsWith(ENDPOINT_ELEMENT)) {
      processGenericProperties(componentModel, modelBeanDefinition);
    }
  }

  private void processGenericProperties(ComponentModel componentModel, AbstractBeanDefinition modelBeanDefinition) {
    // in the case of the endpoint element, all the properties must not be set to the {@code
    // org.mule.compatibility.core.endpoint.UrlEndpointURIBuilder}
    // but to the properties map inside it. So we need to revert previous properties processing in this particular case.
    ManagedMap propertiesMap = new ManagedMap();
    componentModel.getInnerComponents().stream().filter(innerComponent -> {
      ComponentIdentifier identifier = innerComponent.getIdentifier();
      return identifier.equals(SPRING_PROPERTY_IDENTIFIER) || identifier.equals(MULE_PROPERTY_IDENTIFIER);
    }).forEach(propertyComponentModel -> {
      PropertyValue propertyValue = getPropertyValueFromPropertyComponent(propertyComponentModel);
      modelBeanDefinition.getPropertyValues().removePropertyValue(propertyValue.getName());
      propertiesMap.put(propertyValue.getName(), propertyValue.getValue());
    });
    componentModel.getInnerComponents().stream().filter(innerComponent -> {
      return innerComponent.getIdentifier().getName().equals(PROPERTIES_ELEMENT);
    }).findFirst().ifPresent(propertiesComponent -> {
      CommonBeanDefinitionCreator.getPropertyValueFromPropertiesComponent(propertiesComponent).stream().forEach(propertyValue -> {
        propertiesMap.put(propertyValue.getName(), propertyValue.getValue());
      });
    });
    if (!propertiesMap.isEmpty()) {
      modelBeanDefinition.getPropertyValues().addPropertyValue(PROPERTIES_ELEMENT, propertiesMap);
    }
  }

  private void processRedeliveryPolicy(AbstractBeanDefinition modelBeanDefinition, ManagedList messageProcessors) {
    if (messageProcessors != null) {
      for (int i = 0; i < messageProcessors.size(); i++) {
        Object processorDefinition = messageProcessors.get(i);
        if (processorDefinition instanceof AbstractBeanDefinition
            && areMatchingTypes(AbstractRedeliveryPolicy.class, ((AbstractBeanDefinition) processorDefinition).getBeanClass())) {
          messageProcessors.remove(i);
          modelBeanDefinition.getPropertyValues().addPropertyValue("redeliveryPolicy", processorDefinition);
          break;
        }
      }
    }
  }

  private ManagedList processMessageProcessors(ComponentModel componentModel, AbstractBeanDefinition modelBeanDefinition) {
    // Remove response message processor from message processors list
    ManagedList messageProcessors = (ManagedList) modelBeanDefinition.getPropertyValues().get("messageProcessors");
    if (messageProcessors != null) {
      Object lastMessageProcessor = messageProcessors.get(messageProcessors.size() - 1);
      if (lastMessageProcessor instanceof AbstractBeanDefinition) {
        if (areMatchingTypes(MessageProcessorChainFactoryBean.class,
                             ((AbstractBeanDefinition) lastMessageProcessor).getBeanClass())) {
          messageProcessors.remove(messageProcessors.size() - 1);
        }
      }
    }
    // Take the <response> mps and add them.
    componentModel.getInnerComponents().stream().filter(innerComponent -> {
      return innerComponent.getIdentifier().getName().equals("response");
    }).findFirst().ifPresent(responseComponentModel -> {
      ManagedList responseMessageProcessorsBeanList = new ManagedList();
      responseComponentModel.getInnerComponents().forEach(responseProcessorComponentModel -> {
        BeanDefinition beanDefinition = responseProcessorComponentModel.getBeanDefinition();
        responseMessageProcessorsBeanList
            .add(beanDefinition != null ? beanDefinition : responseProcessorComponentModel.getBeanReference());
      });
      BeanDefinitionBuilder beanDefinitionBuilder =
          BeanDefinitionBuilder.genericBeanDefinition(MessageProcessorChainFactoryBean.class);
      beanDefinitionBuilder.addPropertyValue("messageProcessors", responseMessageProcessorsBeanList);
      modelBeanDefinition.getPropertyValues().addPropertyValue("responseMessageProcessors",
                                                               beanDefinitionBuilder.getBeanDefinition());
    });
    return messageProcessors;
  }

  private void processTransformerReferences(ComponentModel componentModel, AbstractBeanDefinition modelBeanDefinition) {
    String transformerRefs = componentModel.getParameters().get("transformer-refs");
    if (!StringUtils.isEmpty(transformerRefs)) {
      String[] refs = transformerRefs.split(TRANSFORMERS_SEPARATOR);
      ManagedList managedList = new ManagedList();
      for (String ref : refs) {
        managedList.add(new RuntimeBeanReference(ref));
      }
      modelBeanDefinition.getPropertyValues().addPropertyValue("transformers", managedList);
    }
    String responseTransformerRefs = componentModel.getParameters().get("responseTransformer-refs");
    if (!StringUtils.isEmpty(responseTransformerRefs)) {
      String[] refs = responseTransformerRefs.split(TRANSFORMERS_SEPARATOR);
      ManagedList managedList = new ManagedList();
      for (String ref : refs) {
        managedList.add(new RuntimeBeanReference(ref));
      }
      modelBeanDefinition.getPropertyValues().addPropertyValue("responseTransformers", managedList);
    }
  }

  private void processAddressParameter(ComponentModel componentModel, AbstractBeanDefinition modelBeanDefinition) {
    Object addressValue = componentModel.getParameters().get("address");
    if (addressValue != null) {
      addUriBuilderPropertyValue(modelBeanDefinition, addressValue);
    } else {
      processSpecificAddressAttribute(componentModel, modelBeanDefinition);
    }
  }

  private void processReferenceParameter(ComponentModel componentModel, AbstractBeanDefinition modelBeanDefinition) {
    if (componentModel.getParameters().containsKey(REFERENCE_ATTRIBUTE)) {
      modelBeanDefinition.getConstructorArgumentValues()
          .addGenericArgumentValue(new RuntimeBeanReference(componentModel.getParameters().get(REFERENCE_ATTRIBUTE)));
    }
  }

  private void addUriBuilderPropertyValue(AbstractBeanDefinition modelBeanDefinition, Object addressValue) {
    BeanDefinitionBuilder uriBuilderBeanDefinition = BeanDefinitionBuilder.genericBeanDefinition(URIBuilder.class);
    uriBuilderBeanDefinition.addConstructorArgValue(addressValue);
    uriBuilderBeanDefinition.addConstructorArgReference(OBJECT_MULE_CONTEXT);
    modelBeanDefinition.getPropertyValues().addPropertyValue("URIBuilder", uriBuilderBeanDefinition.getBeanDefinition());
  }

  private void processSpecificAddressAttribute(ComponentModel componentModel, AbstractBeanDefinition modelBeanDefinition) {
    if (componentModel.getIdentifier().getNamespace().equals("jms")
        && !componentModel.getParameters().containsKey(REFERENCE_ATTRIBUTE)) {
      StringBuilder address = new StringBuilder("jms://");
      if (componentModel.getParameters().containsKey("queue")) {
        address.append(componentModel.getParameters().get("queue"));
      } else {
        address.append("topic/" + componentModel.getParameters().get("topic"));
      }
      addUriBuilderPropertyValue(modelBeanDefinition, address.toString());
    } else if (componentModel.getIdentifier().getNamespace().equals("vm")) {
      if (componentModel.getParameters().containsKey("path")) {
        StringBuilder address = new StringBuilder("vm://");
        address.append(componentModel.getParameters().get("path"));
        addUriBuilderPropertyValue(modelBeanDefinition, address.toString());
      }
    }
  }
}
