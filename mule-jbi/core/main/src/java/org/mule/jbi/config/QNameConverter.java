/* 
* $Header$
* $Revision$
* $Date$
* ------------------------------------------------------------------------------------------------------
* 
* Copyright (c) SymphonySoft Limited. All rights reserved.
* http://www.symphonysoft.com
* 
* The software in this package is published under the terms of the BSD
* style license a copy of which has been included with this distribution in
* the LICENSE.txt file. 
*
*/
package org.mule.jbi.config;

import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.Converter;
import org.apache.commons.digester.Digester;

import javax.xml.namespace.QName;

/**
 * Creates a Qname object from a service string such as foo:myService
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class QNameConverter implements Converter
{
    private Digester digester;

    public QNameConverter(Digester digester) {
        this.digester = digester;
    }

    /**
     * Convert the specified input object into an output object of the specified
     * type.
     *
     * @param type Data type to which this value should be converted
     * @param value The input value to be converted
     * @throws org.apache.commons.beanutils.ConversionException if conversion cannot be performed
     *             successfully
     */
    public Object convert(Class type, Object value)
    {
        if (value == null) {
            throw new ConversionException("No value specified");
        }
        if (type.isInstance(value)) {
            return value;
        }
        String qnameString = value.toString();

        int i = qnameString.indexOf(":");
        String ns = "";
        String nsUri = null;
        String service = qnameString;
        if(i > -1) {
            ns = qnameString.substring(0, i);
            service = qnameString.substring(i+1);
        }
        nsUri = digester.findNamespaceURI(ns);
        if(nsUri != null) {
            return new QName(nsUri, service, ns);
        } else {
            throw new ConversionException("Failed to create QName.  Namespace not found for: '" + ns + "'");
        }
    }
}
