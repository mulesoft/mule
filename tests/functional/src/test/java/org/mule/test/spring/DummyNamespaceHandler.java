/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.spring;

import org.mule.config.spring.handlers.MuleNamespaceHandler;
import org.mule.config.spring.parsers.generic.OrphanDefinitionParser;

public class DummyNamespaceHandler extends MuleNamespaceHandler
{

    public void init()
    {
        registerBeanDefinitionParser("root", new OrphanDefinitionParser(Nestable.class, false));
        registerBeanDefinitionParser("simple", new OrphanDefinitionParser(Nestable.class, false));
        registerBeanDefinitionParser("subclass", new OrphanDefinitionParser(Nestable.class, false));
        registerBeanDefinitionParser("random", new OrphanDefinitionParser(Nestable.class, false));
        registerBeanDefinitionParser("another", new OrphanDefinitionParser(Nestable.class, false));
    }
}
