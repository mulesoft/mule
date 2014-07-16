/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util;

import org.mule.config.i18n.CoreMessages;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>BeanUtils</code> provides functions for altering the way commons BeanUtils
 * works
 */
// @ThreadSafe
public class BeanUtils extends org.apache.commons.beanutils.BeanUtils
{
    public static final String SET_PROPERTIES_METHOD = "setProperties";

    /**
     * logger used by this class
     */
    private static final Log logger = LogFactory.getLog(BeanUtils.class);

    /**
     * Exception safe version of BeanUtils.populate()
     *
     * @param object      the object to set the properties on
     * @param props       the map of properties to set
     * @param logWarnings whether exception warnings should be logged
     */
    public static void populateWithoutFail(Object object, Map props, boolean logWarnings)
    {
        // Check to see if our object has a setProperties method where the properties
        // map should be set
        if (ClassUtils.getMethod(object.getClass(), SET_PROPERTIES_METHOD, new Class[]{Map.class}) != null)
        {
            try
            {
                BeanUtils.setProperty(object, "properties", props);
            }
            catch (Exception e)
            {
                // this should never happen since we explicitly check for the method
                // above
                if (logWarnings)
                {
                    logger.warn("Property: " + SET_PROPERTIES_METHOD + "=" + Map.class.getName()
                            + " not found on object: " + object.getClass().getName());
                }
            }
        }
        else
        {
            for (Iterator iterator = props.entrySet().iterator(); iterator.hasNext();)
            {
                Map.Entry entry = (Map.Entry) iterator.next();

                try
                {
                    BeanUtils.setProperty(object, entry.getKey().toString(), entry.getValue());
                }
                catch (Exception e)
                {
                    if (logWarnings)
                    {
                        logger.warn("Property: " + entry.getKey() + "=" + entry.getValue()
                                + " not found on object: " + object.getClass().getName());
                    }
                }
            }
        }
    }

    /**
     * This will overlay a map of properties on a bean.  This method will validate that all properties are available
     * on the bean before setting the properties
     *
     * @param bean  the bean on which to set the properties
     * @param props a Map of properties to set on the bean
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    public static void populate(Object bean, Map props) throws IllegalAccessException, InvocationTargetException
    {
        // Check to see if our object has a setProperties method where the properties
        // map should be set
        if (ClassUtils.getMethod(bean.getClass(), SET_PROPERTIES_METHOD, new Class[]{Map.class}) != null)
        {
            BeanUtils.setProperty(bean, "properties", props);
        }
        else
        {
            Map master = describe(bean);
            for (Iterator iterator = props.keySet().iterator(); iterator.hasNext();)
            {
                Object o = iterator.next();
                if (!master.containsKey(o))
                {
                    throw new IllegalArgumentException(CoreMessages.propertyDoesNotExistOnObject(o.toString(), bean).getMessage());
                }

            }
            org.apache.commons.beanutils.BeanUtils.populate(bean, props);
        }
    }

    /**
     * The Apache BeanUtils version of this converts all values to String, which is pretty useless, it also includes
     * stuff not defined by the user
     *
     * @param object the object to Describe
     * @return a map of the properties on the object
     */
    public static Map describe(Object object)
    {
        Map props = new HashMap(object.getClass().getDeclaredFields().length);
        for (int i = 0; i < object.getClass().getDeclaredFields().length; i++)
        {
            Field field = object.getClass().getDeclaredFields()[i];
            field.setAccessible(true);
            try
            {
                props.put(field.getName(), field.get(object));
            }
            catch (IllegalAccessException e)
            {
                logger.debug("Unable to read field: " + field.getName() + " on object: " + object);
            }
        }
        return props;
    }

    /**
     * Similar to {@link #describe(Object)} except that it will only populate bean properties where there is a valid
     * getter and setter method. Basically this method will describe a bean and honour its encapsulation.
     *
     * @param object the object to describe
     * @return a map of published properties
     */
    public static Map<String, Object> describeBean(Object object)
    {
        Map<String, Object> props = new HashMap<String, Object>();
        for (int i = 0; i < object.getClass().getMethods().length; i++)
        {
            Method method = object.getClass().getMethods()[i];
            if (method.getName().startsWith("get") || method.getName().startsWith("is"))
            {
                String field = (method.getName().startsWith("is") ? method.getName().substring(2) : method.getName().substring(3));
                String setter = "set" + field;
                try
                {
                    object.getClass().getMethod(setter, method.getReturnType());
                }
                catch (NoSuchMethodException e)
                {
                    logger.debug("Ignoring bean property: " + e.getMessage());
                    continue;
                }
                field = field.substring(0, 1).toLowerCase() + field.substring(1);
                try
                {
                    props.put(field, method.invoke(object));
                }
                catch (Exception e)
                {
                    logger.debug("unable to call bean method: " + method);
                }
            }
        }
        return props;
    }
}
