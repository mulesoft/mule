/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
