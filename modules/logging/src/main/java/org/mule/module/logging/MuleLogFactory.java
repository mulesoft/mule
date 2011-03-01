/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.logging;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogConfigurationException;
import org.apache.commons.logging.impl.SLF4JLogFactory;

/**
 *
 */
public class MuleLogFactory extends SLF4JLogFactory
{

    @Override
    public Log getInstance(Class clazz) throws LogConfigurationException
    {
        return super.getInstance(clazz);
    }
}
