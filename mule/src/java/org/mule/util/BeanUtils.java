/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */

package org.mule.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Iterator;
import java.util.Map;

/**
 * <code>BeanUtils</code> provides functions for altering the way commons
 * BeanUtils works
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class BeanUtils extends org.apache.commons.beanutils.BeanUtils
{
    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(BeanUtils.class);

    /**
     * Exception safe version of BeanUtils.populateWithoutFail
     * 
     * @param object
     * @param props
     */
    public static void populateWithoutFail(Object object, Map props, boolean logWarnings)
    {
        Map.Entry entry;
        for (Iterator iterator = props.entrySet().iterator(); iterator.hasNext();) {
            entry = (Map.Entry) iterator.next();
            try {
                org.apache.commons.beanutils.BeanUtils.setProperty(object, entry.getKey().toString(), entry.getValue());
            } catch (Exception e) {
                if (logWarnings) {
                    logger.warn("Property: " + entry.getKey() + "=" + entry.getValue() + " not found on object: "
                            + object.getClass().getName());
                }
            }
        }
    }
}
