/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.guice.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.specific.ObjectFactoryDefinitionParser;

/**
 * Parsers for handing elements in the guice namespace
 */
public class GuiceNamespaceHandler extends AbstractMuleNamespaceHandler
{
    public void init()
    {
        registerBeanDefinitionParser("object", new ObjectFactoryDefinitionParser(GuiceLookupObjectFactory.class, "objectFactory"));
    }
}
