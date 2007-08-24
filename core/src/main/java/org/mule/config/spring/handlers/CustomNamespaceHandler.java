/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.handlers;

import org.mule.config.spring.parsers.generic.OrphanDefinitionParser;
import org.mule.config.spring.parsers.specific.ServiceDescriptorDefinitionParser;
import org.mule.impl.model.streaming.StreamingModel;
import org.mule.impl.model.direct.DirectModel;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

public class CustomNamespaceHandler extends NamespaceHandlerSupport
{

    public void init()
    {
        registerBeanDefinitionParser("model", new OrphanDefinitionParser(true));
        registerBeanDefinitionParser("service", new ServiceDescriptorDefinitionParser());
    }

}