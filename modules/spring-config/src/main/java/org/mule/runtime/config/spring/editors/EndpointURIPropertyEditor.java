/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.editors;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.endpoint.EndpointException;
import org.mule.runtime.core.endpoint.MuleEndpointURI;

import java.beans.PropertyEditorSupport;

/**
 * Translates a connector name property into the corresponding {@link org.mule.runtime.core.api.transport.Connector}
 * instance.
 */
public class EndpointURIPropertyEditor extends PropertyEditorSupport
{
    private MuleContext muleContext;

    public EndpointURIPropertyEditor(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    @Override
    public void setAsText(String text)
    {
        try
        {
            setValue(new MuleEndpointURI(text, muleContext));
        }
        catch (EndpointException e)
        {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

}
