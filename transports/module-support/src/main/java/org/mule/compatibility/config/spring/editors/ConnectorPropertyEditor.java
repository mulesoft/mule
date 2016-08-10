/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.config.spring.editors;

import org.mule.compatibility.core.api.transport.Connector;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.config.i18n.CoreMessages;

import java.beans.PropertyEditorSupport;

/**
 * Translates a connector name property into the corresponding {@link org.mule.compatibility.core.api.transport.Connector}
 * instance.
 *
 * TODO - Why isn't this simply a reference?
 */
public class ConnectorPropertyEditor extends PropertyEditorSupport implements MuleContextAware
{
    private MuleContext muleContext;

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    @Override
    public void setAsText(String text)
    {

        Connector connector = muleContext.getRegistry().lookupObject(text);

        if (connector == null)
        {
            throw new IllegalArgumentException(CoreMessages.objectNotRegistered("Connector", text).getMessage());
        }
        setValue(connector);
    }
}
