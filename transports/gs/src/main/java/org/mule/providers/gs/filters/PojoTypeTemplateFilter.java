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

import net.jini.core.entry.Entry;

import org.mule.providers.gs.JiniMessage;
import org.mule.umo.UMOMessage;

import java.lang.reflect.InvocationTargetException;

/**
 * TODO document
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class PojoTypeTemplateFilter implements JavaSpaceFilter
{
    private String expectedType;

    public Entry getEntry()
        throws IllegalAccessException, NoSuchMethodException, InvocationTargetException,
        InstantiationException, ClassNotFoundException
    {
        if (expectedType == null)
        {
            return null;
        }

        JiniMessage entry = new JiniMessage();
        entry.setPayloadType(expectedType);
        return entry;
    }

    /**
     * Check a given message against this filter.
     * 
     * @param message a non null message to filter.
     * @return <code>true</code> if the message matches the filter
     */
    public boolean accept(UMOMessage message)
    {
        return true;
    }

    public String getExpectedType()
    {
        return expectedType;
    }

    public void setExpectedType(String expectedType)
    {
        this.expectedType = expectedType;
    }

}
