/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config;

import static org.mule.runtime.module.extension.internal.capability.xml.schema.model.SchemaConstants.MULE_TLS_NAMESPACE;
import static org.mule.runtime.module.extension.internal.capability.xml.schema.model.SchemaConstants.TLS_CONTEXT_TYPE;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.config.spring.parsers.generic.OrphanDefinitionParser;
import org.mule.runtime.module.tls.internal.DefaultTlsContextFactory;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * A {@link PoolingProfileInfrastructureParser} capable of parsing instances of
 * {@link TlsContextFactory}
 *
 * @since 4.0
 */
final class TlsContextInfrastructureParser implements InfrastructureParserDelegate
{

    @Override
    public boolean accepts(Element element)
    {
        return MULE_TLS_NAMESPACE.equals(element.getSchemaTypeInfo().getTypeNamespace()) &&
               TLS_CONTEXT_TYPE.getLocalPart().equals(element.getLocalName());
    }

    @Override
    public void parse(Element element, ManagedMap<Class<?>, BeanDefinition> managedMap, ParserContext parserContext)
    {
        managedMap.put(TlsContextFactory.class, new OrphanDefinitionParser(DefaultTlsContextFactory.class, true).parse(element, parserContext));
    }
}
