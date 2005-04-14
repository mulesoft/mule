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
package org.mule.routing.filters.logic;

import org.mule.umo.UMOFilter;

/**
 * <code>AndFilter</code> accepts only if the leftFilter and rightFilter filter accept
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class AndFilter implements UMOFilter
{
    private UMOFilter leftFilter;
    private UMOFilter rightFilter;

    public AndFilter()
    {
    }


    public AndFilter(UMOFilter left, UMOFilter right)
    {
        this.leftFilter = left;
        this.rightFilter = right;
    }


    public void setLeftFilter(UMOFilter leftFilter)
    {
        this.leftFilter = leftFilter;
    }

    public void setRightFilter(UMOFilter rightFilter)
    {
        this.rightFilter = rightFilter;
    }


    public UMOFilter getLeftFilter()
    {
        return leftFilter;
    }

    public UMOFilter getRightFilter()
    {
        return rightFilter;
    }

    public boolean accept(Object object)
    {
        return leftFilter.accept(object) && rightFilter.accept(object);
    }
}
