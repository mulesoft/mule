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
 */

package org.mule.config.converters;

import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.beanutils.Converter;
import org.mule.umo.UMOTransactionFactory;
import org.mule.util.ClassHelper;

/**
 * <code>TransactionFactoryConverter</code> TODO
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class TransactionFactoryConverter implements Converter
{
    // --------------------------------------------------------- Public Methods

    /**
     * Convert the specified input object into an output object of the specified
     * type.
     * 
     * @param type Data type to which this value should be converted
     * @param value The input value to be converted
     * @throws org.apache.commons.beanutils.ConversionException if conversion
     *             cannot be performed successfully
     */
    public Object convert(Class type, Object value)
    {
        if (value == null) {
            throw new ConversionException("No value specified");
        }
        if (value instanceof UMOTransactionFactory) {
            return (value);
        }
        try {
            Object factory = ClassHelper.loadClass(value.toString(), getClass()).newInstance();
            return factory;
        } catch (Exception e) {
            throw new ConversionException(e);
        }
    }
}
