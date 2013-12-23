/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.pgp.config;

import org.mule.api.config.MuleProperties;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.generic.NamedDefinitionParser;
import org.mule.config.spring.parsers.generic.ParentDefinitionParser;
import org.mule.config.spring.parsers.specific.SecurityFilterDefinitionParser;
import org.mule.module.pgp.KeyBasedEncryptionStrategy;
import org.mule.module.pgp.PGPSecurityProvider;
import org.mule.module.pgp.filters.PGPSecurityFilter;
import org.mule.util.SecurityUtils;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

public class PgpNamespaceHandler extends NamespaceHandlerSupport
{

    public void init()
    {
        registerRestrictedBeanDefinitionParser("security-manager", new NamedDefinitionParser(MuleProperties.OBJECT_SECURITY_MANAGER));
        registerRestrictedBeanDefinitionParser("security-provider", new ChildDefinitionParser("provider", PGPSecurityProvider.class));
        registerRestrictedBeanDefinitionParser("security-filters", new ParentDefinitionParser());
        registerRestrictedBeanDefinitionParser("security-filter", new SecurityFilterDefinitionParser(PGPSecurityFilter.class));
        registerRestrictedBeanDefinitionParser("keybased-encryption-strategy", new ChildDefinitionParser("encryptionStrategy", KeyBasedEncryptionStrategy.class));
    }

    private void registerRestrictedBeanDefinitionParser(String id, final BeanDefinitionParser beanDefinitionParser)
    {
        registerBeanDefinitionParser(id, new BeanDefinitionParser()
        {
            @Override
            public BeanDefinition parse(Element element, ParserContext parserContext)
            {
                if (SecurityUtils.isFipsSecurityModel())
                {
                    throw new IllegalStateException(String.format("Cannot use PGP module when using FIPS security model. " +
                                                                  "Element %s is not allowed.", element.getNodeName()));
                }
                else
                {
                    return beanDefinitionParser.parse(element, parserContext);
                }
            }
        });
    }

}
