/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json.transformers;

import java.io.Serializable;

import net.sf.ezmorph.MorpherRegistry;
import net.sf.ezmorph.bean.MorphDynaBean;
import net.sf.ezmorph.bean.MorphDynaClass;

import org.apache.commons.beanutils.DynaBean;
import org.apache.commons.beanutils.DynaClass;

/**
 * A wrapper for the {@link net.sf.ezmorph.bean.MorphDynaBean} object that allows for nested object keys i.e.
 * user.name will return the name property on the user object.
 */
public class JsonDynaBean implements DynaBean, Serializable
{
    private MorphDynaBean morphDynaBean;

    public JsonDynaBean(MorphDynaBean morphDynaBean)
    {
        this.morphDynaBean = morphDynaBean;
    }

    public boolean contains(String name, String key)
    {
        return morphDynaBean.contains(name, key);
    }

    public boolean equals(Object obj)
    {
        return morphDynaBean.equals(obj);
    }

    public Object get(String name)
    {
       int i = name.indexOf(".");
        String key;
        if(i > 0)
        {
            key = name.substring(0, i);
        }
        else
        {
            key = name;
        }

        Object o = morphDynaBean.get(key);
        if(o instanceof MorphDynaBean && i > 0)
        {
            return new JsonDynaBean((MorphDynaBean)o).get(name.substring(i+1));
        }
        else
        {
            return o;
        }
    }

    public Object get(String name, int index)
    {
        return morphDynaBean.get(name, index);
    }

    public Object get(String name, String key)
    {
        return morphDynaBean.get(name, key);
    }

    public DynaClass getDynaClass()
    {
        return morphDynaBean.getDynaClass();
    }

    public MorpherRegistry getMorpherRegistry()
    {
        return morphDynaBean.getMorpherRegistry();
    }

    public void remove(String name, String key)
    {
        morphDynaBean.remove(name, key);
    }

    public void set(String name, int index, Object value)
    {
        morphDynaBean.set(name, index, value);
    }

    public void set(String name, Object value)
    {
        morphDynaBean.set(name, value);
    }

    public void set(String name, String key, Object value)
    {
        morphDynaBean.set(name, key, value);
    }

    public void setDynaBeanClass(MorphDynaClass dynaClass)
    {
        morphDynaBean.setDynaBeanClass(dynaClass);
    }

    public void setMorpherRegistry(MorpherRegistry morpherRegistry)
    {
        morphDynaBean.setMorpherRegistry(morpherRegistry);
    }

    public String toString()
    {
        return morphDynaBean.toString();
    }
}
