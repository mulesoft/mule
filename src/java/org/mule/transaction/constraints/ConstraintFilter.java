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
package org.mule.transaction.constraints;

import org.mule.routing.filters.UMOEventFilter;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOFilter;

/**
 * <code>ConstraintFilter</code> TODO
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class ConstraintFilter implements UMOFilter, Cloneable
{
    private UMOFilter eventFilter = null;

    public ConstraintFilter()
    {
        eventFilter = new UMOEventFilter();
    }

    public final boolean accept(Object object)
    {
        if(eventFilter.accept(object)) {
            return accept((UMOEvent)object);
        } else {
            return false;
        }
    }

    protected boolean accept(UMOEvent event)
    {
        return true;
    }

    public Object clone() throws CloneNotSupportedException
    {
        return new ConstraintFilter();
    }


}
