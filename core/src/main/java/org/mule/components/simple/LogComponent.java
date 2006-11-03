/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.components.simple;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.umo.UMOEventContext;
import org.mule.umo.lifecycle.Callable;
import org.mule.util.StringMessageUtils;

/**
 * <code>LogComponent</code> Simply logs the content (or content length if it is a
 * large message)
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class LogComponent implements Callable, LogService
{
    private static Log logger = LogFactory.getLog(LogComponent.class);

    public Object onCall(UMOEventContext context) throws Exception
    {
        String contents = context.getMessageAsString();
        String msg = "Message received in component: " + context.getComponentDescriptor().getName();
        msg = StringMessageUtils.getBoilerPlate(msg + ". Content is: '"
                                                + StringMessageUtils.truncate(contents, 100, true) + "'");
        log(msg);
        return null;
    }

    public void log(String message)
    {
        logger.info(message);
    }
}
