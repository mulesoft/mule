/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.springconfig.editors;

import org.mule.api.MuleContext;
import org.mule.api.transport.Connector;
import org.mule.config.i18n.CoreMessages;

import java.beans.PropertyEditorSupport;

/**
 * Translates a connector name property into the corresponding {@link org.mule.api.transport.Connector}
 * instance.
 *
 * TODO - Why isn't this simply a reference?
 */
public class ConnectorPropertyEditor extends PropertyEditorSupport
{
    private MuleContext muleContext;

    public ConnectorPropertyEditor(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    public void setAsText(String text)
    {

        Connector connector = muleContext.getRegistry().lookupConnector(text);

        if (connector == null)
        {
            throw new IllegalArgumentException(CoreMessages.objectNotRegistered("Connector", text).getMessage());
        }
        setValue(connector);
    }
}
