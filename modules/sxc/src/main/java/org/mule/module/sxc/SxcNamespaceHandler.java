/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.sxc;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.collection.ChildMapEntryDefinitionParser;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.specific.FilterDefinitionParser;
import org.mule.config.spring.parsers.specific.RouterDefinitionParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SxcNamespaceHandler extends AbstractMuleNamespaceHandler
{
    private transient final Logger logger = LoggerFactory.getLogger(getClass());

    public void init()
    {
        logger.warn("SXC module is deprecated and will be removed in Mule 4.0.");
        registerBeanDefinitionParser("filtering-router",
                new RouterDefinitionParser(SxcFilteringOutboundRouter.class)    );
         registerBeanDefinitionParser("namespace",
             new ChildMapEntryDefinitionParser("namespaces", "prefix", "uri"));

        registerBeanDefinitionParser("filter", 
                new FilterDefinitionParser(SxcFilter.class));
    }
}
