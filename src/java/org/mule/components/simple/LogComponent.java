/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.components.simple;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.umo.UMOEventContext;
import org.mule.umo.lifecycle.Callable;
import org.mule.util.StringMessageHelper;

/**
 * <code>LogComponent</code> Simply logs the content (or content length is it is a large message)
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class LogComponent implements Callable, LogService
{
    private static transient Log logger = LogFactory.getLog(FunctionalTestComponent.class);

    public Object onCall(UMOEventContext context) throws Exception
    {
        String contents = context.getMessageAsString();
        String msg = "Message Received in component: " + context.getComponentDescriptor().getName();
        if(contents.length() > 100) {
            msg = StringMessageHelper.getBoilerPlate(msg + ". Content length is: " + contents.length());
        }else {
            msg = StringMessageHelper.getBoilerPlate(msg + ". Content is: " + contents);
        }
        log(msg);
        return null;
    }

    public void log(String message) {
        logger.info(message);
    }
}
