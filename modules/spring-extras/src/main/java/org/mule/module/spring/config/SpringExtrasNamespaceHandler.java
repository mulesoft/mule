/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.spring.config;

import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.module.spring.objectstore.SpringCacheObjectStore;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

public class SpringExtrasNamespaceHandler extends NamespaceHandlerSupport
{

    public void init()
    {
        registerBeanDefinitionParser("spring-store", new ChildDefinitionParser("store", SpringCacheObjectStore.class));
    }
}
