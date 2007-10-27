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

import org.mule.umo.NamedObject;

/**
 * <code>PropertyExtractor</code> extracts a property from the message in a generic
 * way. i.e. composite properties can be pulled and aggregated depending on this
 * strategy. This can be used to extract Correlation Ids, Message Ids etc.
 *
 * These objects are used to execute property expressions (usually on the
 * current message) at runtime to extracta dynamic value.
 */
public interface PropertyExtractor extends NamedObject
{
    /**
     * Extracts a single property from the message
     * 
     * @param expression the property expression or expression
     * @param message the message to extract from
     * @return the result of the extraction or null if the property was not found
     */
    Object getProperty(String expression, Object message);

}
