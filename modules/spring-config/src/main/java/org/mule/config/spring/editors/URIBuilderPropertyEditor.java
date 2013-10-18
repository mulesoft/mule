/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.editors;

import org.mule.api.MuleContext;
import org.mule.endpoint.URIBuilder;

import java.beans.PropertyEditorSupport;

/**
 * Translates a connector name property into the corresponding {@link org.mule.api.transport.Connector}
 * instance.
 */
public class URIBuilderPropertyEditor extends PropertyEditorSupport
{
    private MuleContext muleContext;

    public URIBuilderPropertyEditor(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    public void setAsText(String text)
    {
        setValue(new URIBuilder(text, muleContext));
    }

}
