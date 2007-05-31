/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.editors;

import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.umo.endpoint.EndpointException;

import java.beans.PropertyEditorSupport;

/**
 * Translates a connecotr name property into the corresponding {@link org.mule.umo.provider.UMOConnector}
 * instance.
 */
public class EndpointURIPropertyEditor extends PropertyEditorSupport
{
    public void setAsText(String text)
    {
        try
        {
            setValue(new MuleEndpointURI(text));
        }
        catch (EndpointException e)
        {
            throw new IllegalArgumentException(e.getMessage());
        }
    }
}
