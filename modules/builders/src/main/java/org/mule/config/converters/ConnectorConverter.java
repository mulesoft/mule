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

import org.mule.RegistryContext;
import org.mule.umo.provider.UMOConnector;

import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.Converter;

/**
 * <code>ConnectorConverter</code> TODO
 */
public class ConnectorConverter implements Converter
{

    // --------------------------------------------------------- Public Methods

    /**
     * Convert the specified input object into an output object of the specified
     * type.
     * 
     * @param type Data type to which this value should be converted
     * @param value The input value to be converted
     * @throws ConversionException if conversion cannot be performed successfully
     */
    public Object convert(Class type, Object value)
    {
        if (value == null)
        {
            throw new ConversionException("No value specified");
        }
        if (value instanceof UMOConnector)
        {
            return (value);
        }
        try
        {
            UMOConnector c = RegistryContext.getRegistry().lookupConnector(value.toString());
            if (c == null)
            {
                throw new ConversionException("UMOConnector: " + value.toString()
                                              + " has not been registered with Mule");
            }
            return c;
        }
        catch (Exception e)
        {
            throw new ConversionException(e);
        }
    }
}
