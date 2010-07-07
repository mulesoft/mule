/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.construct;

import org.mule.AbstractSimpleFlowConstuct;
import org.mule.api.MuleContext;
import org.mule.api.component.Component;
import org.mule.processor.builder.ChainMessageProcessorBuilder;

/**
 * In-out SOA-style simple service, with no outbound router. Always fully
 * synchronous.
 */
public class SimpleService extends AbstractSimpleFlowConstuct
{
    private Component component;

    public SimpleService(MuleContext muleContext)
    {
        super(muleContext);
    }

    public void setComponent(Component component)
    {
        this.component = component;
    }

    public Component getComponent()
    {
        return component;
    }

    @Override
    protected void addMessageProcessors(ChainMessageProcessorBuilder builder)
    {
        component.setFlowConstruct(this);
        builder.chain(component);
    }
}
