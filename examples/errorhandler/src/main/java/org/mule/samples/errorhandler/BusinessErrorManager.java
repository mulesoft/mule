/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.samples.errorhandler;

import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOException;
import org.mule.umo.lifecycle.Callable;
import org.mule.util.StringMessageUtils;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The <code>BusinessErrorManager</code> is the UMO Component that processes 
 * exceptions of type org.mule.samples.errorhandler.exceptions.BusinessException.
 * The business method simply reports the errors and stops any further processing.
 */
public class BusinessErrorManager implements Callable
{
    /** logger used by this class */
    private static final Log logger = LogFactory.getLog(BusinessErrorManager.class);

    public Object onCall(UMOEventContext context) throws UMOException
    {
        ErrorMessage msg = (ErrorMessage)context.getTransformedMessage();
        // Do something with the error message
        List msgs = new ArrayList();

        msgs.add(LocaleMessage.businessErrorManagerError());
        msgs.add(LocaleMessage.errorDetail(msg.getException().getDetailMessage()));
        msgs.add(LocaleMessage.errorClass(msg.getException().getClass()));

        logger.info("\n" + StringMessageUtils.getBoilerPlate(msgs, '*', 80));
        context.setStopFurtherProcessing(true);
        return null;
    }

}
