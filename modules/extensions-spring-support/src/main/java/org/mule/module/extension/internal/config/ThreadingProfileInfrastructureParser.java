/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.config;

import static org.mule.api.config.MuleProperties.OBJECT_DEFAULT_SERVICE_THREADING_PROFILE;
import static org.mule.config.spring.parsers.specific.NameConstants.MULE_NAMESPACE;
import static org.mule.module.extension.internal.capability.xml.schema.model.SchemaConstants.MULE_ABSTRACT_THREADING_PROFILE_TYPE;
import static org.w3c.dom.TypeInfo.DERIVATION_EXTENSION;
import org.mule.api.config.ThreadingProfile;
import org.mule.config.ChainedThreadingProfile;
import org.mule.config.spring.parsers.generic.OrphanDefinitionParser;
import org.mule.config.spring.parsers.processors.ConstructorReference;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * A {@link InfrastructureParserDelegate} capable of parsing instances of
 * {@link ThreadingProfile}
 *
 * @since 4.0
 */
final class ThreadingProfileInfrastructureParser implements InfrastructureParserDelegate
{

    @Override
    public boolean accepts(Element element)
    {
        return element.getSchemaTypeInfo().isDerivedFrom(MULE_NAMESPACE, MULE_ABSTRACT_THREADING_PROFILE_TYPE.getLocalPart(), DERIVATION_EXTENSION);
    }

    @Override
    public void parse(Element element, ManagedMap<Class<?>, BeanDefinition> managedMap, ParserContext parserContext)
    {
        managedMap.put(ThreadingProfile.class, getThredingProfileParser().parse(element, parserContext));
    }

    private OrphanDefinitionParser getThredingProfileParser()
    {
        OrphanDefinitionParser parser = new OrphanDefinitionParser(ChainedThreadingProfile.class, true);
        parser.addMapping("poolExhaustedAction", ThreadingProfile.POOL_EXHAUSTED_ACTIONS)
                .registerPostProcessor(new ConstructorReference(OBJECT_DEFAULT_SERVICE_THREADING_PROFILE));
        return parser;
    }
}
