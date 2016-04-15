/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.xpath;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPathConstants;

/**
 * An Enum listing the possible return types defined in the JAXP (JSR-206) without actually
 * coupling to it. The {@link #toQName()} method allows correlating each value
 * to the standard {@link QName} which can be used in the JAXP API.
 *
 * @since 3.6.0
 */
public enum XPathReturnType
{

    BOOLEAN
            {
                protected QName toQName()
                {
                    return XPathConstants.BOOLEAN;
                }
            },
    NUMBER
            {
                @Override
                protected QName toQName()
                {
                    return XPathConstants.NUMBER;
                }
            },

    STRING
            {
                @Override
                protected QName toQName()
                {
                    return XPathConstants.STRING;
                }
            },

    NODESET
            {
                @Override
                protected QName toQName()
                {
                    return XPathConstants.NODESET;
                }
            },

    NODE
            {
                @Override
                protected QName toQName()
                {
                    return XPathConstants.NODE;
                }
            };

    /**
     * Returns the {@link QName} related to each value
     *
     * @return a {@link QName}
     */
    protected abstract QName toQName();
}
