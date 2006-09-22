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

import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.Converter;

import javax.xml.namespace.QName;

import java.util.StringTokenizer;

/**
 * <code>TransformerConverter</code> will obtain an endpoint name and convert
 * it to a <code>UMOConnector</code> instance by looking up the endpoint in
 * the <code>MuleManager</code>.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class QNameConverter implements Converter {

    boolean explicit = false;

    public QNameConverter() {
        super();
    }

    public QNameConverter(boolean explicit) {
        this.explicit = explicit;
    }
    // --------------------------------------------------------- Public Methods

    /**
     * Convert the specified input object into an output object of the specified
     * type.
     *
     * @param type  Data type to which this value should be converted
     * @param value The input value to be converted
     * @throws org.apache.commons.beanutils.ConversionException
     *          if conversion cannot be performed
     *          successfully
     */
    public Object convert(Class type, Object value) {
        if (value == null) {
            throw new ConversionException("No value specified");
        }
        if (value instanceof QName) {
            return (value);
        }
        String val = value.toString();
        if (val.startsWith("qname{")) {
            return parseQName(val.substring(6, val.length() - 1));
        } else if (!explicit) {
            return parseQName(val);
        } else {
            return new QName(val);
        }
    }

    protected QName parseQName(String val) {
        StringTokenizer st = new StringTokenizer(val, ":");
        String[] elements = new String[4];
        int i = 0;
        while (st.hasMoreTokens()) {
            elements[i] = st.nextToken();
            i++;
        }

        QName qname = null;
        if (i == 1) {
            qname = new QName(elements[0]);
        } else if (i == 2) {
            qname = new QName(elements[0], elements[1]);
        } else if (i == 3) {
            qname = new QName(elements[1] + ":" + elements[2], elements[0]);
        } else if (i == 4) {
            qname = new QName(elements[2] + ":" + elements[3], elements[1], elements[0]);
        }
        return qname;
    }
}
