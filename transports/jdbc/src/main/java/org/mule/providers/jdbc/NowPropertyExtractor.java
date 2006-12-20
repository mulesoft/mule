/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jdbc;

import java.sql.Timestamp;

import org.mule.util.properties.PropertyExtractor;

/**
 * Recognises the property 'NOW' to mean a TimeStamp with the current time
 */
public class NowPropertyExtractor implements PropertyExtractor
{

    public Object getProperty(String name, Object message)
    {
        if (name.equalsIgnoreCase("now"))
        {
            return new Timestamp(System.currentTimeMillis());
        }
        return null;
    }
}
