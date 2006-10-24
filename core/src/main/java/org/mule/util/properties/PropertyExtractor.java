/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.properties;

/**
 * <code>PropertyExtractor</code> extracts a property from the message in a generic
 * way. i.e. composite properties can be pulled and aggregated depending on this
 * strategy. This can be used to extract Correlation Ids, Message Ids etc.
 */
public interface PropertyExtractor
{
    /**
     * Extracts a single property from the message
     * 
     * @param name the property name or expression
     * @param message the message to extract from
     * @return the result of the extraction or null if the property was not found
     */
    Object getProperty(String name, Object message);

}
