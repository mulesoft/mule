/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.scripting.config;

import org.mule.component.DefaultInterfaceBinding;
import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.generic.TextDefinitionParser;
import org.mule.config.spring.parsers.specific.BindingDefinitionParser;
import org.mule.config.spring.parsers.specific.ComponentDefinitionParser;
import org.mule.config.spring.parsers.specific.MessageProcessorDefinitionParser;
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


