/* 
 * $Id$
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
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.manager.UMOManager;
import org.mule.util.SgmlCodec;

/**
 * <code>TransformerConverter</code>will obtain an endpoint name and convert
 * it to a <code>UMOEndpoint</code> instance by looking up the proivder from
 * the <code>MuleManager</code>.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class EndpointURIConverter implements Converter
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
        UMOManager manager = MuleManager.getInstance();
        if (value == null) {
            throw new ConversionException("No value specified");
        }
        if (value instanceof UMOEndpointURI) {
            return value;
        }
        try {
            String endpoint = manager.lookupEndpointIdentifier(value.toString(), value.toString());
            endpoint = SgmlCodec.decodeString(endpoint);
            return new MuleEndpointURI(endpoint);
        } catch (Exception e) {
            throw new ConversionException(e);
        }
    }
}
