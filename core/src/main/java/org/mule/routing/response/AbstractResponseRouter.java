/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing.response;

import org.mule.api.routing.ResponseRouter;
import org.mule.routing.AbstractRouter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>AbstractResponseRouter</code> is a base class for all Response Routers
 */

public abstract class AbstractResponseRouter extends AbstractRouter implements ResponseRouter
{
    protected final Log logger = LogFactory.getLog(getClass());


}
