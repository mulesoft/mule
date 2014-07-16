/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.el;

import org.dom4j.Node;
import org.mule.mvel2.ConversionHandler;
import org.mule.mvel2.conversion.StringCH;

class XMLToStringConversionHandler implements ConversionHandler
{

    private StringCH delegate = new StringCH();

    @Override
    public Object convertFrom(Object in)
    {
        if (in instanceof Node)
        {
            return ((Node) in).getText();
        }
        else if (in instanceof org.w3c.dom.Node)
        {
            return ((org.w3c.dom.Node) in).getNodeValue();
        }
        else
        {
            return delegate.convertFrom(in);
        }
    }

    @Override
    public boolean canConvertFrom(@SuppressWarnings("rawtypes") Class cls)
    {
        return delegate.canConvertFrom(cls);
    }
}
