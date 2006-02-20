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
package org.mule.umo;

import org.mule.umo.provider.UMOMessageAdapter;

import java.util.Map;

/**
 * <code>UMOMessage</code> represents a message payload. The Message comprises
 * of the payload itself and properties associated with the payload.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public interface UMOMessage extends UMOMessageAdapter
{
    /**
     * Adds a map of properties to associated with this message
     * 
     * @param properties the properties add to this message
     */
    void addProperties(Map properties);

    /**
     * Removes all properties on this message
     */
    void clearProperties();

    /**
     * Returns a map of all properties on this message
     * 
     * @return a map of all properties on this message
     */
    Map getProperties();

    UMOMessageAdapter getAdapter();
}
