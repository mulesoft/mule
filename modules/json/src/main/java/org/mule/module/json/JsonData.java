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

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.net.URL;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * A wrapper for the {@link org.codehaus.jackson.JsonNode} object that
 * allows for nested object keys i.e. user/name will return the name property on
 * the user object.
 *
 * There is no 'xpath' for JSON yet (though I expect Jackson to do implement this at some point).  This class provides
 * a simple way to navigate a Json data structure.
 * To select a child entry use -
 * <code>
 * person/name
 * </code>
 *
 * to access array data, use square braces with an index value i.e.
 * <code>
 * person/addresses[0]/postcode
 *
 * or
 *
 * [0]/arrayElement
 * </code>
 *
 * Also, multi-dimensional arrays can be accessed using:
 * <code>
 * filters[1]/init[1][1]
 * </code>
 *
 */
public class JsonData implements Serializable
{
    private JsonNode node;

    public JsonData(JsonNode node)
    {
        this.node = node;
    }

    public JsonData(URL node) throws IOException
    {
        this(node.openStream());
    }

    public JsonData(InputStream node) throws IOException
    {
        this.node = new ObjectMapper().readTree(node);
    }

    public JsonData(Reader node) throws IOException
    {
        this.node = new ObjectMapper().readTree(node);
    }

    public JsonData(String node) throws IOException
    {
        this(new StringReader(node));
    }

    @Override
    public boolean equals(Object obj) 
    {
        return node.equals(obj);
    }

    public JsonNode get(int index)
    {
        return node.get(index);
    }


    public boolean isArray() 
    {
        return node.isArray();
    }

    public Object get(String name) 
    {
        String key = null;
        int index = -1;
        int x = name.indexOf("[");
        int y = -1;
        if (x >= 0) 
        {
            y = name.indexOf("]");
            key = name.substring(x + 1, y);
            if (NumberUtils.isDigits(key)) 
            {
                index = Integer.valueOf(key);
                key = null;
            }

        }
        int offset = 0;
        if(name.startsWith("'"))
        {
            offset = name.indexOf("'", 1);
        }

        int i = name.indexOf("/", offset);

        String objectName;
        if (x > 0)
        {
            objectName = name.substring(0, x);
        } 
        else if (i > 0) 
        {
            objectName = name.substring(0, i);
        } 
        else 
        {
            objectName = name;
        }

        if (isArray() && !objectName.startsWith("[")) 
        {
            throw new IllegalArgumentException(
                    "Object is an array, but a name of the object is given: "
                            + objectName);
        }

        //unquote the string
        if(objectName.startsWith("'"))
        {
            objectName = objectName.substring(1, objectName.length() -1);
        }
        JsonNode o;
        if (key != null) 
        {

           o = null; //morphDynaBean.get(objectName, key);
        } 
        else if (index > -1 && !objectName.startsWith("[")) 
        {
            o = node.get(objectName).get(index);
        }
        else if (index > -1) 
        {
            o = node.get(index);
        } 
        else 
        {
            o = node.get(objectName);
        }

        if(o==null)
        {
            throw new IllegalArgumentException(objectName + " does not exist");
        }

        if (!o.isValueNode() && i > 0)
        {
            return new JsonData(o).get(name.substring(i + 1)); //2
        }
        if (!o.isValueNode() && y > 0)
        {
            return new JsonData(o).get(name.substring(y + 1));
        } 
//        else if (o instanceof List && i > 0)
//        {
//            return new JsonData((List) o).get(name.substring(i + 1)); //2
//        }
//        else if (o instanceof List && y > 0)
//        {
//            return new JsonData((List) o).get(name.substring(y + 1));
//        }
        else 
        {
            return o.getValueAsText();
        }
    }

//    public Object get(String name, int index)
//    {
//        return morphDynaBean.get(name, index);
//    }
//
//    public Object get(String name, String key)
//    {
//        return morphDynaBean.get(name, key);
//    }
//
//    public DynaClass getDynaClass()
//    {
//        return morphDynaBean.getDynaClass();
//    }
//
//    public MorpherRegistry getMorpherRegistry()
//    {
//        return morphDynaBean.getMorpherRegistry();
//    }
//
//    public void remove(String name, String key)
//    {
//        morphDynaBean.remove(name, key);
//    }
//
//    public void set(String name, int index, Object value)
//    {
//        morphDynaBean.set(name, index, value);
//    }
//
//    public void set(String name, Object value)
//    {
//        morphDynaBean.set(name, value);
//    }
//
//    public void set(String name, String key, Object value)
//    {
//        morphDynaBean.set(name, key, value);
//    }
//
//    public void setDynaBeanClass(MorphDynaClass dynaClass)
//    {
//        morphDynaBean.setDynaBeanClass(dynaClass);
//    }
//
//    public void setMorpherRegistry(MorpherRegistry morpherRegistry)
//    {
//        morphDynaBean.setMorpherRegistry(morpherRegistry);
//    }

    @Override
    public String toString()
    {
        return node.toString();
//        if (morphDynaBean != null)
//        {
//            return morphDynaBean.toString();
//        }
//        else if (entries != null && entries.size() > 0)
//        {
//            StringBuffer sb = new StringBuffer();
//            for (int i = 0; i < entries.size(); i++)
//            {
//                sb.append(entries.get(i));
//            }
//            return sb.toString();
//        }
//        return ""; // avoid returning null
    }
}
