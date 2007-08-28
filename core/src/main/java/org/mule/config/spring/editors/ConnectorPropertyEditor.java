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

import org.mule.RegistryContext;
import org.mule.config.i18n.CoreMessages;
import org.mule.umo.provider.UMOConnector;

import java.beans.PropertyEditorSupport;

/**
 * Translates a connecotr name property into the corresponding {@link org.mule.umo.provider.UMOConnector}
 * instance.
 */
public class ConnectorPropertyEditor extends PropertyEditorSupport
{
    public void setAsText(String text)
    {

        UMOConnector connector = RegistryContext.getRegistry().lookupConnector(text);

        if (connector == null)
        {
            throw new IllegalArgumentException(CoreMessages.objectNotRegistered("Connector", text).getMessage());
        }
        setValue(connector);
    }
}