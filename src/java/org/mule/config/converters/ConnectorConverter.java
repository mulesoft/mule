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

package org.mule.config.converters;

import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.Converter;
import org.mule.MuleManager;
import org.mule.umo.provider.UMOConnector;

/**
 * <code>TransformerConverter</code> will obtain an endpoint name and
 * convert it to a <code>UMOConnector</code> instance by looking up the endpoint in the
 * <code>MuleManager</code>.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ConnectorConverter implements Converter
{

    // --------------------------------------------------------- Public Methods

    /**
     * Convert the specified input object into an output object of the
     * specified type.
     *
     * @param type  Data type to which this value should be converted
     * @param value The input value to be converted
     * @throws ConversionException if conversion cannot be performed
     *                             successfully
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
            UMOConnector c = MuleManager.getInstance().lookupConnector(value.toString());
            if (c == null)
            {
                throw new ConversionException("UMOConnector: " + value.toString() + " has not been registered with Mule");
            }
            return c;
        }
        catch (Exception e)
        {
            throw new ConversionException(e);
        }
    }
}
