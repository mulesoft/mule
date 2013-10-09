/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.editors;

import java.beans.PropertyEditorSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.xml.namespace.QName;

/**
 * This handles qname{....} syntax as used in stockquote-soap-config.xml
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

    @Override
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
            case 0 :
            {
                return null;
            }
            case 1 :
            {
                return new QName((String) elements.get(0));
            }
            case 2 :
            {
                return new QName((String) elements.get(0), (String) elements.get(1));
            }
            case 3 :
            {
                return new QName((String) elements.get(1) + ":" + (String) elements.get(2), (String) elements.get(0));
            }
            default :
            {
                String prefix = (String) elements.get(0);
                String local = (String) elements.get(1);
                // namespace can have multiple colons in it, so just assume the rest
                // is a namespace
                String ns = val.substring(prefix.length() + local.length() + 2);
                return new QName(ns, local, prefix);
            }
        }
    }

    public static QName convert(String value)
    {
        QNamePropertyEditor editor = new QNamePropertyEditor();
        editor.setAsText(value);
        return (QName) editor.getValue();
    }

}
