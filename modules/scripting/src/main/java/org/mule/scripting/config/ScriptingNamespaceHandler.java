/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.scripting.config;

import org.mule.config.spring.handlers.AbstractIgnorableNamespaceHandler;


public class ScriptingNamespaceHandler extends AbstractIgnorableNamespaceHandler
{
    public void init()
    {
        registerIgnoredElement("lang");

        registerBeanDefinitionParser("script", new ScriptDefinitionParser(false));
        registerBeanDefinitionParser("groovyRefreshable", new GroovyRefreshableBeanBuilderParser(false));
    }

}


