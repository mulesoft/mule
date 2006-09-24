/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util;

import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>BeanUtils</code> provides functions for altering the way commons BeanUtils
 * works
 */
// @Immutable
public class BeanUtils extends org.apache.commons.beanutils.BeanUtils
{
    /**
     * logger used by this class
     */
    private static transient final Log logger = LogFactory.getLog(BeanUtils.class);

    /**
     * Exception safe version of BeanUtils.populateWithoutFail
     */
    public static void populateWithoutFail(Object object, Map props, boolean logWarnings)
    {
        for (Iterator iterator = props.entrySet().iterator(); iterator.hasNext();)
        {
            Map.Entry entry = (Map.Entry)iterator.next();

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
