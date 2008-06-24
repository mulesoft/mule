/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;

public class TestNamespaceHandler extends AbstractMuleNamespaceHandler
{

    public void init()
    {
        registerBeanDefinitionParser("component", new TestComponentDefinitionParser());
        //This is handled by the TestComponentDefinitionParser
        registerIgnoredElement("return-data");
        registerIgnoredElement("callback");
    }

}
