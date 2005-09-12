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
package org.mule.config;

import org.mule.umo.UMOMessage;

import java.util.List;
import java.util.Map;

/**
 * <code>PropertyExtractor</code> extracts a property from the message in a
 * generic way. i.e. composite properties can be pulled and aggregated depending
 * on this strategy. This can be used to extract Correlation Ids, Message Ids
 * etc.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public interface PropertyExtractor
{
    /**
     * Extracts a single property from the message
     * @param name the property name or expression
     * @param message the message to extract from
     * @return the result of the extraction or null if the property was not found
     */
    Object getProperty(String name, UMOMessage message);

    /**
     * Where a property extract must first parse the message body of a message this
     * implementation meothd will be more efficient as the parsed content is cached
     * for each property extraction
     * @param names a list of property names or expressions
     * @param message the message to extract from
     * @return a map of key/value pairs where the key is the name or expression
     */
    Map getProperties(List names, UMOMessage message);
}
