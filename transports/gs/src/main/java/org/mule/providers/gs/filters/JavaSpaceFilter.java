/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.gs.filters;

import java.lang.reflect.InvocationTargetException;

import net.jini.core.entry.Entry;

import org.mule.umo.UMOFilter;

/**
 * TODO document
 */
public interface JavaSpaceFilter extends UMOFilter
{

    public Entry getEntry()
        throws IllegalAccessException, NoSuchMethodException, InvocationTargetException,
        InstantiationException, ClassNotFoundException;
}
