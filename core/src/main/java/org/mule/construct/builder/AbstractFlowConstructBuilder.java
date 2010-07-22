/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.construct.builder;

import java.beans.ExceptionListener;

import org.mule.construct.AbstractFlowConstruct;
import org.mule.service.DefaultServiceExceptionStrategy;

public abstract class AbstractFlowConstructBuilder
{
    protected static final DefaultServiceExceptionStrategy DEFAULT_SERVICE_EXCEPTION_STRATEGY = new DefaultServiceExceptionStrategy();

    protected String name;

    protected ExceptionListener exceptionListener;

    // TODO (DDO) pull more setters from SimpleServiceBuilder

    protected void addExceptionListener(AbstractFlowConstruct flowConstruct)
    {
        if (exceptionListener != null)
        {
            flowConstruct.setExceptionListener(exceptionListener);
        }
        else
        {
            flowConstruct.setExceptionListener(DEFAULT_SERVICE_EXCEPTION_STRATEGY);
        }
    }
}
