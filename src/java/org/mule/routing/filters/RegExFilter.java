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

import java.util.regex.Pattern;

/**
 * <code>RegExFilter</code> is used to match a rgular expression against a string argument.
 * 
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public class RegExFilter implements UMOFilter
{
    private Pattern pattern;

    public RegExFilter()
    {
    }


    public RegExFilter(String pattern) {
        this.pattern = Pattern.compile(pattern);
    }

    public boolean accept(Object object)
    {
        if(object==null) return false;
        return pattern.matcher(object.toString()).find();
    }

    public String getPattern()
    {
        return (pattern==null ? null : pattern.pattern());
    }

    public void setPattern(String pattern)
    {
        this.pattern = Pattern.compile(pattern);
    }


}
