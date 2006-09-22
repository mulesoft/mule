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
import org.mule.umo.transformer.UMOTransformer;

/**
 * <code>TransformerConverter</code> will obtain a transformer name and
 * convert it to a transformer instance by looking up the transformer from the
 * <code>MuleManager</code>.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class TransformerConverter implements Converter
{
    /**
     * Convert the specified input object into an output object of the specified
     * type.
     * 
     * @param type Data type to which this value should be converted
     * @param value The input value to be converted
     * @throws ConversionException if conversion cannot be performed
     *             successfully
     */
    public Object convert(Class type, Object value)
    {
        if (value == null) {
            throw new ConversionException("No value specified");
        }
        if (value instanceof UMOTransformer) {
            return value;
        }
        return null;
    }
}
