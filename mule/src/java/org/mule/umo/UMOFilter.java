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

/**
 * The <code>UMOFilter</code> interface allows UMOMessage filtering.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public interface UMOFilter
{
    /**
     * Check a given message against this filter.
     * 
     * @param message a non null message to filter.
     * @return <code>true</code> if the message matches the filter
     */
    boolean accept(UMOMessage message);
}
