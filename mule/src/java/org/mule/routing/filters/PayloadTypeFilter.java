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

import org.mule.umo.UMOFilter;

/**
 * <code>PayloadTypeFilter</code> filters based on the type of the object received.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class PayloadTypeFilter implements UMOFilter
{
    private Class expectedType;

    public PayloadTypeFilter()
    {
    }

    public PayloadTypeFilter(Class expectedType)
    {
        this.expectedType = expectedType;
    }


    public boolean accept(Object object)
    {
        return expectedType.isAssignableFrom(object.getClass());
    }

    public Class getExpectedType()
    {
        return expectedType;
    }

    public void setExpectedType(Class expectedType)
    {
        this.expectedType = expectedType;
    }
}
