/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.editors;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.api.transport.Connector;
import org.mule.endpoint.URIBuilder;

import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;

/**
 * The preferred way to configure property editors in Spring 2/3 is to implement a registrar
 */
public class MulePropertyEditorRegistrar implements PropertyEditorRegistrar, MuleContextAware
{
    private MuleContext muleContext;

    public void setMuleContext(MuleContext context)
    {
        muleContext = context;
    }

    public void registerCustomEditors(PropertyEditorRegistry registry)
    {
        registry.registerCustomEditor(Connector.class, new ConnectorPropertyEditor(muleContext));
        registry.registerCustomEditor(URIBuilder.class, new URIBuilderPropertyEditor(muleContext));
    }
}
