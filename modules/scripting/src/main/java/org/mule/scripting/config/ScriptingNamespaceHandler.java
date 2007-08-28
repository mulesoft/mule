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

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

public class ScriptingNamespaceHandler extends NamespaceHandlerSupport 
{
    public void init()
    {
        registerBeanDefinitionParser("script", new ScriptDefinitionParser(false));
        registerBeanDefinitionParser("groovyRefreshable", new GroovyRefreshableBeanBuilderParser(false));
    }

}


