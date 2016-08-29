/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.spring;

import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.FILTER_REFERENCE_ELEMENT;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.MESSAGE_FILTER_ELEMENT;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.REFERENCE_ATTRIBUTE;
import static org.mule.runtime.config.spring.dsl.processor.xml.CoreXmlNamespaceInfoProvider.CORE_NAMESPACE_NAME;
import static org.mule.runtime.config.spring.dsl.processor.xml.XmlCustomAttributeHandler.from;
import org.mule.runtime.core.config.ComponentIdentifier;
import org.mule.runtime.config.spring.dsl.model.ComponentModel;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.routing.filter.Filter;
import org.mule.runtime.core.routing.MessageFilter;

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;

/**
 * Processor of the chain of responsibility that knows how to create the
 * {@link org.springframework.beans.factory.config.BeanDefinition} for an filter reference element.
 *
 * @since 4.0
 */
class FilterReferenceBeanDefinitionCreator extends BeanDefinitionCreator {

  private static final ComponentIdentifier FILTER_REFERENCE_IDENTIFIER =
      new ComponentIdentifier.Builder().withNamespace(CORE_NAMESPACE_NAME).withName(FILTER_REFERENCE_ELEMENT).build();

  @Override
  public boolean handleRequest(CreateBeanDefinitionRequest createBeanDefinitionRequest) {
    ComponentModel componentModel = createBeanDefinitionRequest.getComponentModel();
    if (componentModel.getIdentifier().equals(FILTER_REFERENCE_IDENTIFIER)) {
      if (from(componentModel).getNode().getParentNode().getNodeName().equals(MESSAGE_FILTER_ELEMENT)) {
        componentModel.setType(Filter.class);
        componentModel.setBeanReference(new RuntimeBeanReference(componentModel.getParameters().get(REFERENCE_ATTRIBUTE)));
      } else {
        componentModel.setType(MessageProcessor.class);
        BeanDefinitionBuilder beanDefinitionBuilder =
            org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition(MessageFilter.class);
        beanDefinitionBuilder.addConstructorArgReference(componentModel.getParameters().get(REFERENCE_ATTRIBUTE));
        componentModel.setBeanDefinition(beanDefinitionBuilder.getBeanDefinition());
      }
      return true;
    }
    return false;
  }
}
