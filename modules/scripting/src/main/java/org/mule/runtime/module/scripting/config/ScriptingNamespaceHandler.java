/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.scripting.config;

import org.mule.runtime.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.runtime.config.spring.parsers.generic.TextDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.BindingDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.ComponentDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.MessageProcessorDefinitionParser;
import org.mule.runtime.core.component.DefaultInterfaceBinding;
import org.mule.runtime.module.scripting.component.ScriptComponent;
import org.mule.runtime.module.scripting.filter.ScriptFilter;
import org.mule.runtime.module.scripting.transformer.ScriptTransformer;


public class ScriptingNamespaceHandler extends AbstractMuleNamespaceHandler
{
    @Override
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


