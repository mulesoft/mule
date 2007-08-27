/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.properties;

import org.mule.umo.UMOMessage;
import org.mule.util.StringUtils;

import java.lang.reflect.InvocationTargetException;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Checks the payload object for a bean property matching the property name
 */
public class PayloadPropertyExtractor implements PropertyExtractor
{
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    public Object getProperty(String name, Object message)
    {
        Object payload = message;
        if (message instanceof UMOMessage)
        {
            payload = ((UMOMessage) message).getPayload();
        }
        Object value = null;
        try
        {
            if ((PropertyUtils.getPropertyDescriptor(payload, name) != null))
            {
                value = PropertyUtils.getProperty(payload, name);
                if (value == null)
                {
                    value = StringUtils.EMPTY;
                }
            }
        }
        catch (IllegalAccessException e)
        {
            logger.warn("Failed to read property: " + name, e);
        }
        catch (InvocationTargetException e)
        {
            logger.warn("Failed to read property: " + name, e);
        }
        catch (NoSuchMethodException e)
        {
            // will never happen
        }
        return value;
    }
}
