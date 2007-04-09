/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.converters;


import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.xml.namespace.QName;

import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.Converter;

/**
 * <code>QNameConverter</code> TODO document properly; see QNameConverterTestCase
 * for now
 * @deprecated use QNamePropertyEditor instead
 * @see org.mule.config.spring.editors.QNamePropertyEditor
 */
public class QNameConverter implements Converter
{

    boolean explicit = false;

    public QNameConverter()
    {
        super();
    }

    public QNameConverter(boolean explicit)
    {
        this.explicit = explicit;
    }

    // --------------------------------------------------------- Public Methods

    /**
     * Convert the specified input object into an output object of the specified
     * type.
     * 
     * @param type Data type to which this value should be converted
     * @param value The input value to be converted
     * @throws org.apache.commons.beanutils.ConversionException if conversion cannot
     *             be performed successfully
     */
    public Object convert(Class type, Object value)
    {
        if (value == null)
        {
            throw new ConversionException("No value specified");
        }

        if (value instanceof QName)
        {
            return (value);
        }

        String val = value.toString();
        if (val.startsWith("qname{"))
        {
            return parseQName(val.substring(6, val.length() - 1));
        }
        else if (!explicit)
        {
            return parseQName(val);
        }
        else
        {
            return new QName(val);
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
                return new QName((String) elements.get(0));
            case 2 :
                return new QName((String) elements.get(0), (String) elements.get(1));
            case 3 :
                return new QName((String) elements.get(1) + ":" + (String) elements.get(2),
                    (String) elements.get(0));
            case 4 :
                return new QName((String) elements.get(2) + ":" + (String) elements.get(3),
                    (String) elements.get(1), (String) elements.get(0));
            default :
                return null;
        }
    }

}
