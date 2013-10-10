/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.xml.el;

import org.dom4j.Node;
import org.mvel2.ConversionHandler;
import org.mvel2.conversion.StringCH;

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
