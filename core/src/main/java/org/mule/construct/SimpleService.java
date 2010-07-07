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

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.component.Component;
import org.mule.api.source.MessageSource;

import edu.emory.mathcs.backport.java.util.Collections;

/**
 * In-out SOA-style simple service, with no outbound router. Always fully
 * synchronous.
 */
public class SimpleService extends SimpleFlowConstuct
{
    private final Component component;

    public SimpleService(MuleContext muleContext,
                         String name,
                         MessageSource inboundMessageSource,
                         Component component) throws MuleException
    {
        super(muleContext, name);
        this.inboundMessageSource = inboundMessageSource;
        this.component = component;
        setMessageProcessors(Collections.singletonList(component));
    }

    public Component getComponent()
    {
        return component;
    }
}
