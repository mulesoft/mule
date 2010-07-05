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

import org.mule.MessageExchangePattern;

import java.beans.PropertyEditorSupport;

public class MessageExchangePatternPropertyEditor extends PropertyEditorSupport
{
    @Override
    public void setAsText(String text) throws IllegalArgumentException
    {
        String epString = text.toUpperCase().replace('-', '_');
        setValue(MessageExchangePattern.valueOf(epString));
    }
}
