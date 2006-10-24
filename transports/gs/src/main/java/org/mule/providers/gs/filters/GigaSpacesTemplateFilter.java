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

import org.apache.commons.beanutils.BeanUtils;
import org.mule.providers.gs.GigaSpacesEntryConverter;
import org.mule.util.ClassUtils;

public class GigaSpacesTemplateFilter extends JavaSpaceTemplateFilter
{
    private GigaSpacesEntryConverter converter = new GigaSpacesEntryConverter();

    public Entry getEntry()
        throws IllegalAccessException, NoSuchMethodException, InvocationTargetException,
        InstantiationException, ClassNotFoundException
    {
        if (entry == null)
        {
            if (expectedType == null)
            {
                return null; // Match all template
            }
            Object entryType = ClassUtils.instanciateClass(expectedType, ClassUtils.NO_ARGS);
            if (fields.size() > 0)
            {
                BeanUtils.populate(entryType, fields);
            }
            entry = converter.toEntry(entryType, null);
        }
        return entry;
    }

}
