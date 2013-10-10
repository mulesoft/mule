/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.jersey.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
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

        ComponentDefinitionParser parser = new ComponentDefinitionParser(JerseyResourcesComponent.class);
        registerBeanDefinitionParser("resources", parser);
    }
}
