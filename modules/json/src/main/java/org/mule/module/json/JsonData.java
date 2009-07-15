/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json;

import org.mule.util.NumberUtils;

import java.io.Serializable;
import java.util.List;

import net.sf.ezmorph.MorphException;
import net.sf.ezmorph.MorpherRegistry;
import net.sf.ezmorph.bean.MorphDynaBean;
import net.sf.ezmorph.bean.MorphDynaClass;

import org.apache.commons.beanutils.DynaBean;
import org.apache.commons.beanutils.DynaClass;

/**
 * A wrapper for the {@link net.sf.ezmorph.bean.MorphDynaBean} object that allows for nested object keys i.e.
 * user.name will return the name property on the user object.
 */
public class JsonData implements DynaBean, Serializable
{
    private MorphDynaBean morphDynaBean;
    private List entries;

    public JsonData(List entries)
    {
        this.entries = entries;
    }

    public JsonData(MorphDynaBean morphDynaBean)
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

    public Object get(int index)
    {
        return entries.get(index);
    }

    public boolean isArray()
    {
        return entries!=null;
    }

    public Object get(String name)
    {
        String key = null;
        int index = -1;
        int x = name.indexOf("[");
        int y = -1;
        if(x >= 0)
        {
            y =  name.indexOf("]");
            key = name.substring(x+1, y);
            if(NumberUtils.isDigits(key))
            {
                index = Integer.valueOf(key);
                key = null;
            }

        }
       int i = name.indexOf("->");
        String objectName;
        if(x > 0)
            {
                objectName = name.substring(0, x);
            }
        else if(i > 0)
        {

            objectName = name.substring(0, i);
            
        }
        else
        {
            objectName = name;
        }
        if(isArray() && !objectName.startsWith("["))
        {
            throw new MorphException("Object is an array, but a nae of the object is given: " + objectName);      
        }

        Object o;
        if(key!=null)
        {
            o = morphDynaBean.get(objectName, key);
        }
        else if(index > -1 && !objectName.startsWith("["))
        {
            o = morphDynaBean.get(objectName, index);
        }
        else if(index > -1)
        {
            o = get(index);
        }
        else
        {
            o = morphDynaBean.get(objectName);
        }

        if(o instanceof MorphDynaBean && i > 0)
        {
            return new JsonData((MorphDynaBean)o).get(name.substring(i+2));
        }
        if(o instanceof MorphDynaBean && y > 0)
        {
            return new JsonData((MorphDynaBean)o).get(name.substring(y+1));
        }
        else if(o instanceof List && i > 0)
        {
            return new JsonData((List)o).get(name.substring(i+2));
        }
        else if(o instanceof List && y > 0)
        {
            return new JsonData((List)o).get(name.substring(y+1));
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
