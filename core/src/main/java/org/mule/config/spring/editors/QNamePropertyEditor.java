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

import java.beans.PropertyEditorSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.xml.namespace.QName;

/**
 * TODO
 */
public class QNamePropertyEditor extends PropertyEditorSupport
{
    private boolean explicit = false;

    public QNamePropertyEditor()
    {
        super();
    }
    
    public QNamePropertyEditor(boolean explicit)
    {
        this();
        this.explicit = explicit;
    }

    //@Override
    public void setAsText(String text) throws IllegalArgumentException
    {

        if (text.startsWith("qname{"))
        {
            setValue(parseQName(text.substring(6, text.length() - 1)));
        }
        else if (!explicit)
        {
            setValue(parseQName(text));
        }
        else
        {
            setValue(new QName(text));
        }
    }

    protected QName parseQName(String val)
    {
        StringTokenizer st = new StringTokenizer(val, ":");
        List elements = new ArrayList();

        while (st.hasMoreTokens())
        {
            elements.add(st.nextToken());
        }

        switch (elements.size())
        {
            case 1 :
            {
                return new QName((String)elements.get(0));
            }
            case 2 :
            {
                return new QName((String)elements.get(0), (String)elements.get(1));
            }
            case 3 :
            {
                return new QName((String)elements.get(1) + ":" + (String)elements.get(2),
                    (String)elements.get(0));
            }
            case 4 :
            {
                return new QName((String)elements.get(2) + ":" + (String)elements.get(3),
                    (String)elements.get(1), (String)elements.get(0));
            }
            default :
            {
                return null;
            }
        }
    }
}
