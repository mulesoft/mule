/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.scripting.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.generic.TextDefinitionParser;
import org.mule.config.spring.parsers.specific.BindingDefinitionParser;
import org.mule.config.spring.parsers.specific.ComponentDefinitionParser;
import org.mule.config.spring.parsers.specific.TransformerDefinitionParser;
import org.mule.module.scripting.component.ScriptComponent;
import org.mule.module.scripting.transformer.ScriptTransformer;
import org.mule.routing.nested.DefaultNestedRouter;


public class ScriptingNamespaceHandler extends AbstractMuleNamespaceHandler
{
    public void init()
    {
        registerBeanDefinitionParser("script", new ScriptDefinitionParser());
        registerBeanDefinitionParser("text", new TextDefinitionParser("scriptText"));
        registerBeanDefinitionParser("component", new ComponentDefinitionParser(ScriptComponent.class));
        registerMuleBeanDefinitionParser("binding", new BindingDefinitionParser("nestedRouter.routers", DefaultNestedRouter.class)).addCollection("nestedRouter.routers");

        registerBeanDefinitionParser("transformer", new TransformerDefinitionParser(ScriptTransformer.class));

        // For Spring-based scripting support
        registerIgnoredElement("lang");
        registerBeanDefinitionParser("groovy-refreshable", new GroovyRefreshableBeanBuilderParser(false));
    }
}


