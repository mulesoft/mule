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

import org.mule.config.spring.parsers.specific.URIBuilder;

import java.beans.PropertyEditorSupport;

/**
 * Translates a connector name property into the corresponding {@link org.mule.umo.provider.UMOConnector}
 * instance.
 */
public class URIBuilderPropertyEditor extends PropertyEditorSupport
{

    public void setAsText(String text)
    {
        setValue(new URIBuilder(text));
    }

}