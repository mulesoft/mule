/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.modules.xml.functional;

import java.beans.ExceptionListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class HideExceptions implements ExceptionListener
{

    protected transient Log logger = LogFactory.getLog(getClass());

    public void exceptionThrown(Exception e)
    {
        logger.debug("Hiding exception: " + e);
        logger.debug("(see config for test - some exceptions expected)");
    }

}

