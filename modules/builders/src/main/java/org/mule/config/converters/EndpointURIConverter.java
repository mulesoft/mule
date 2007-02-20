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

import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.util.XMLEntityCodec;

import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.Converter;

/**
 * <code>EndpointURIConverter</code> TODO
 */
public class EndpointURIConverter implements Converter
{

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

        if (value instanceof UMOEndpointURI)
        {
            return value;
        }

        try
        {
            String endpoint = XMLEntityCodec.decodeString(value.toString());
            return new MuleEndpointURI(endpoint);
        }
        catch (Exception e)
        {
            throw new ConversionException(e);
        }
    }

}
