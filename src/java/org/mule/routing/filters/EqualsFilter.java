/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.routing.filters;

import org.mule.umo.UMOFilter;

/**
 * <code>EqualsFilter</code> is a filer for comparing two objects using the equals() method.
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public class EqualsFilter implements UMOFilter
{
    private Object pattern;

    public EqualsFilter()
    {
    }


    public EqualsFilter(Object compareTo) {
        this.pattern = compareTo;
    }

    public boolean accept(Object object)
    {
        if(object==null && pattern ==null) return true;
        if(object==null || pattern ==null) return false;
        return pattern.equals(object);
    }

    public Object getPattern()
    {
        return pattern;
    }

    public void setPattern(Object pattern)
    {
        this.pattern = pattern;
    }


}
