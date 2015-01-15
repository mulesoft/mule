/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.scripting.config;

import org.mule.component.DefaultInterfaceBinding;
import org.mule.module.springconfig.handlers.AbstractMuleNamespaceHandler;
import org.mule.module.springconfig.parsers.generic.TextDefinitionParser;
import org.mule.module.springconfig.parsers.specific.BindingDefinitionParser;
import org.mule.module.springconfig.parsers.specific.ComponentDefinitionParser;
import org.mule.module.springconfig.parsers.specific.MessageProcessorDefinitionParser;
import org.mule.module.scripting.component.ScriptComponent;
import org.mule.module.scripting.filter.ScriptFilter;
import org.mule.module.scripting.transformer.ScriptTransformer;


public class ScriptingNamespaceHandler extends AbstractMuleNamespaceHandler
{
    public void init()
    {
        registerBeanDefinitionParser("script", new ScriptDefinitionParser());
        registerBeanDefinitionParser("text", new TextDefinitionParser("scriptText"));
        registerBeanDefinitionParser("component", new ComponentDefinitionParser(ScriptComponent.class));
        registerMuleBeanDefinitionParser("java-interface-binding", new BindingDefinitionParser("interfaceBinding", DefaultInterfaceBinding.class)).addCollection("bindingCollection.routers");

        registerBeanDefinitionParser("transformer", new MessageProcessorDefinitionParser(ScriptTransformer.class));
        registerBeanDefinitionParser("filter", new MessageProcessorDefinitionParser(ScriptFilter.class));

        // For Spring-based scripting support
        registerIgnoredElement("lang");
        registerBeanDefinitionParser("groovy-refreshable", new GroovyRefreshableBeanBuilderParser(false));
    }
}


