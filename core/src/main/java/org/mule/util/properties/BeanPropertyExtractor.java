/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.properties;

import org.mule.umo.UMOMessage;

/**
 * Recognises the property 'PAYLOAD' to mean the whole message payload
 */
public class BeanPropertyExtractor implements PropertyExtractor
{

    public Object getProperty(String name, Object message)
    {
        if (name.equalsIgnoreCase("payload"))
        {
            if (message instanceof UMOMessage)
            {
                return ((UMOMessage) message).getPayload();
            }
            return message;
        }
        return null;
    }
}
