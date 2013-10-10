/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.sxc;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.collection.ChildMapEntryDefinitionParser;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.specific.FilterDefinitionParser;
import org.mule.config.spring.parsers.specific.RouterDefinitionParser;

public class SxcNamespaceHandler extends AbstractMuleNamespaceHandler
{
    public void init()
    {        
        registerBeanDefinitionParser("filtering-router",
                new RouterDefinitionParser(SxcFilteringOutboundRouter.class)    );
         registerBeanDefinitionParser("namespace",
             new ChildMapEntryDefinitionParser("namespaces", "prefix", "uri"));

        registerBeanDefinitionParser("filter", 
                new FilterDefinitionParser(SxcFilter.class));
    }
}
