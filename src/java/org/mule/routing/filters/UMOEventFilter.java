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
package org.mule.routing.filters;

import org.mule.umo.UMOEvent;
import org.mule.umo.UMOFilter;

/**
 * <code>UMOEventFilter</code> is a filter for determining if the object is a UMOEvent.
 * This filter will allways be used in conjunction with at least one other filter.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class UMOEventFilter implements UMOFilter
{
    public boolean accept(Object object)
    {
        return object instanceof UMOEvent;
    }
}
