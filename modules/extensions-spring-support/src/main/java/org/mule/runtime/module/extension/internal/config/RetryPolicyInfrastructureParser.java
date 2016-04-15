/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.config;

import static org.mule.config.spring.parsers.specific.NameConstants.MULE_NAMESPACE;
import static org.mule.module.extension.internal.capability.xml.schema.model.SchemaConstants.MULE_ABSTRACT_RECONNECTION_STRATEGY_TYPE;
import static org.w3c.dom.TypeInfo.DERIVATION_EXTENSION;

import org.mule.runtime.core.api.retry.RetryPolicyTemplate;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * A {@link InfrastructureParserDelegate} capable of parsing instances of
 * {@link RetryPolicyTemplate}
 *
 * @since 4.0
 */
final class RetryPolicyInfrastructureParser implements InfrastructureParserDelegate
{

    @Override
    public boolean accepts(Element element)
    {
        return element.getSchemaTypeInfo().isDerivedFrom(MULE_NAMESPACE, MULE_ABSTRACT_RECONNECTION_STRATEGY_TYPE.getLocalPart(), DERIVATION_EXTENSION);
    }

    @Override
    public void parse(Element element, ManagedMap<Class<?>, BeanDefinition> managedMap, ParserContext parserContext)
    {
        managedMap.put(RetryPolicyTemplate.class, parserContext.getDelegate().parseCustomElement(element));
    }
}
