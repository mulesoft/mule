/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
    }
}
