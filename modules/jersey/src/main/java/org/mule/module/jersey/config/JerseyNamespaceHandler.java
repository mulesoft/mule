/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.jersey.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.collection.ChildListEntryDefinitionParser;
import org.mule.config.spring.parsers.collection.ChildMapEntryDefinitionParser;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.specific.ComponentDefinitionParser;
import org.mule.module.jersey.JerseyResourcesComponent;

public class JerseyNamespaceHandler extends AbstractMuleNamespaceHandler
{
    @Override
    public void init()
    {
        ChildDefinitionParser exceptionMapperParser = new ChildDefinitionParser("exceptionMapper",
            null, null, true);
        registerBeanDefinitionParser("exception-mapper", exceptionMapperParser);

        ChildDefinitionParser contextResolverParser = new ChildDefinitionParser("contextResolver",
            null, null, true);
        registerBeanDefinitionParser("context-resolver", contextResolverParser);

        registerBeanDefinitionParser("property", new ChildMapEntryDefinitionParser("properties"));
        registerBeanDefinitionParser("package", new ChildListEntryDefinitionParser("packages", "packageName"));

        ComponentDefinitionParser parser = new ComponentDefinitionParser(JerseyResourcesComponent.class);
        registerBeanDefinitionParser("resources", parser);
    }
}
