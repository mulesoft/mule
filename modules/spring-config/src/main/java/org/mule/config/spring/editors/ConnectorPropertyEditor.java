/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.editors;

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
