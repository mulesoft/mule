/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.config;

import static org.mule.module.extension.internal.capability.xml.schema.model.SchemaConstants.MULE_ABSTRACT_POOLING_PROFILE_TYPE;
import static org.mule.module.extension.internal.capability.xml.schema.model.SchemaConstants.MULE_NAMESPACE;
import static org.w3c.dom.TypeInfo.DERIVATION_EXTENSION;
import org.mule.api.config.PoolingProfile;
import org.mule.config.spring.parsers.generic.OrphanDefinitionParser;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/**
 * A {@link InfrastructureParserDelegate} capable of parsing instances of
 * {@link PoolingProfile}
 *
 * @since 4.0
 */
final class PoolingProfileInfrastructureParser implements InfrastructureParserDelegate
{

    @Override
    public boolean accepts(Element element)
    {
        return element.getSchemaTypeInfo().isDerivedFrom(MULE_NAMESPACE, MULE_ABSTRACT_POOLING_PROFILE_TYPE.getLocalPart(), DERIVATION_EXTENSION);
    }

    @Override
    public void parse(Element element, ManagedMap<Class<?>, BeanDefinition> managedMap, ParserContext parserContext)
    {
        managedMap.put(PoolingProfile.class, getPoolingProfileParser().parse(element, parserContext));
    }

    //TODO: MULE-9047 should have a better way of parsing this
    private BeanDefinitionParser getPoolingProfileParser()
    {
        OrphanDefinitionParser poolingProfileParser = new OrphanDefinitionParser(PoolingProfile.class, true);
        poolingProfileParser.addMapping("initialisationPolicy", PoolingProfile.POOL_INITIALISATION_POLICIES);
        poolingProfileParser.addMapping("exhaustedAction", PoolingProfile.POOL_EXHAUSTED_ACTIONS);
        return poolingProfileParser;
    }
}
