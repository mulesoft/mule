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
 */
package org.mule.routing;

import org.mule.config.MuleProperties;
import org.mule.config.SimplePropertyExtractor;
import org.mule.umo.UMOMessage;

/**
 * <code>DefaultPropertiesExtractor</code> is a default implementation used
 * for getting the Correlation information from a message. This object is only
 * used when getting a specific property to be set on the message. When reading
 * the property the getProperty(...) or the direct property accessor will be
 * used i.e. message.getCorrelationId() or
 * message.getProperty(MuleProperties.MULE_CORRELATION_ID_PROPERTY)
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class CorrelationPropertiesExtractor extends SimplePropertyExtractor
{
    public final Object getProperty(String name, UMOMessage message)
    {
        Object result = null;
        if (MuleProperties.MULE_CORRELATION_ID_PROPERTY.equals(name)) {
            result = getCorrelationId(message);
        } else if (MuleProperties.MULE_MESSAGE_ID_PROPERTY.equals(name)) {
            result = getMessageId(message);
        } else {
            throw new IllegalArgumentException("Property name: " + name
                    + " not recognised by the Correlation Property Extractor");
        }
        if (result == null) {
            throw new NullPointerException("Property Extractor cannot return a null value. Extractor is: "
                    + getClass().getName());
        }
        return result;
    }

    public String getMessageId(UMOMessage message)
    {
        return message.getUniqueId();
    }

    public String getCorrelationId(UMOMessage message)
    {
        String id = message.getCorrelationId();
        if(id==null) {
            id = message.getUniqueId();
        }
        return id;
    }
}
