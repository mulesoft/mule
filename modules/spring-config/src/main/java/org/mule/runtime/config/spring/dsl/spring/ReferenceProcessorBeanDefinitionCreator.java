/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.spring;

import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.PROCESSOR_REFERENCE_ELEMENT;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.REFERENCE_ATTRIBUTE;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.TRANSFORMER_REFERENCE_ELEMENT;
import static org.mule.runtime.config.spring.dsl.processor.xml.CoreXmlNamespaceInfoProvider.CORE_NAMESPACE_NAME;
import org.mule.runtime.config.spring.dsl.model.ComponentIdentifier;
import org.mule.runtime.config.spring.dsl.model.ComponentModel;
import org.mule.runtime.core.api.processor.MessageProcessor;

import org.springframework.beans.factory.config.RuntimeBeanReference;

/**
 * Processor of the chain of responsibility that knows how to create the {@link org.springframework.beans.factory.config.BeanDefinition}
 * for a transformer or processor reference element.
 *
 * @since 4.0
 */
class ReferenceProcessorBeanDefinitionCreator extends BeanDefinitionCreator
{
    private static final ComponentIdentifier PROCESSOR_REF_IDENTIFIER = new ComponentIdentifier.Builder().withNamespace(CORE_NAMESPACE_NAME).withName(PROCESSOR_REFERENCE_ELEMENT).build();
    private static final ComponentIdentifier TRANSFORMER_REF_IDENTIFIER = new ComponentIdentifier.Builder().withNamespace(CORE_NAMESPACE_NAME).withName(TRANSFORMER_REFERENCE_ELEMENT).build();

    @Override
    public boolean handleRequest(CreateBeanDefinitionRequest createBeanDefinitionRequest)
    {
        ComponentModel componentModel = createBeanDefinitionRequest.getComponentModel();
        if (componentModel.getIdentifier().equals(PROCESSOR_REF_IDENTIFIER) || componentModel.getIdentifier().equals(TRANSFORMER_REF_IDENTIFIER))
        {
            componentModel.setType(MessageProcessor.class);
            componentModel.setBeanReference(new RuntimeBeanReference(componentModel.getParameters().get(REFERENCE_ATTRIBUTE)));
            return true;
        }
        return false;
    }
}
