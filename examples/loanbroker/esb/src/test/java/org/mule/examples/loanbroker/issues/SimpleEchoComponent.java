/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.examples.loanbroker.issues;

import org.mule.umo.UMOEventContext;
import org.mule.umo.lifecycle.Callable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SimpleEchoComponent implements Callable
{

    private Log logger = LogFactory.getLog(getClass());

    public Object onCall(UMOEventContext context) throws Exception
    {
        Object message = context.getTransformedMessage();
        logger.debug("received " + message);

        return message;
    }

}
