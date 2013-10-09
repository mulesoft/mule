/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
        ErrorMessage msg = (ErrorMessage)context.getMessage().getPayload();
        // Do something with the error message
        List<String> msgs = new ArrayList<String>();

        msgs.add(LocaleMessage.businessErrorManagerError());
        msgs.add(LocaleMessage.errorDetail(msg.getException().getDetailMessage()));
        msgs.add(LocaleMessage.errorClass(msg.getException().getClass()));

        logger.info("\n" + StringMessageUtils.getBoilerPlate(msgs, '*', 80));
        context.setStopFurtherProcessing(true);
        return null;
    }

}
