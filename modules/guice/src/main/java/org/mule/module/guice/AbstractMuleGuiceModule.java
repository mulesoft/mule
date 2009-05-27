/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Key;

import java.util.Map;
import java.util.HashMap;

/**
 * A mule specific Guice module that allows bindings to be associated with a string key.  Note that Guice binds all
 * objects by {@link Class} or {@link com.google.inject.Key}. Mule adds support for String bindings only for use with the
 * Mule Xml Configuration. We recommend that you shouldn't use string bindings for any other
 * purpose since it kinda goes against the principles of the guice project.
 */
public abstract class AbstractMuleGuiceModule extends AbstractModule
{

    StringBindings stringBindings = new StringBindings();

    public AbstractMuleGuiceModule()
    {
        configureStringBindings(stringBindings);
    }

    Map<String, Object> getStringBindings()
    {
        return stringBindings.getBindings();
    }

    protected abstract void configureStringBindings(StringBindings stringBindings);

    protected class StringBindings
    {
        private Map<String, Object> stringBindings = new HashMap<String, Object>();

        protected void bindString(String name, Class type)
        {
            stringBindings.put(name, type);
        }

        protected void bindString(String name, Key key)
        {
            stringBindings.put(name, key);
        }

        Map<String, Object> getBindings()
        {
            return stringBindings;
        }
    }
}
