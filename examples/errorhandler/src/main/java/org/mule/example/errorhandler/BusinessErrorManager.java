/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.example.errorhandler;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.Callable;
import org.mule.util.StringMessageUtils;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The <code>BusinessErrorManager</code> is a Service that processes 
 * exceptions of type org.mule.example.errorhandler.exceptions.BusinessException.
 * The business method simply reports the errors and stops any further processing.
 */
public class BusinessErrorManager implements Callable
{
    /** logger used by this class */
    private static final Log logger = LogFactory.getLog(BusinessErrorManager.class);

    public Object onCall(MuleEventContext context) throws MuleException
    {
        ErrorMessage msg = (ErrorMessage)context.transformMessage();
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
