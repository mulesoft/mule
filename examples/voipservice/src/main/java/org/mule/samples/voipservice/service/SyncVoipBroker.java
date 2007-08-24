/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.samples.voipservice.service;

import org.mule.impl.MuleMessage;
import org.mule.impl.RequestContext;
import org.mule.samples.voipservice.interfaces.AddressValidation;
import org.mule.samples.voipservice.to.CreditProfileTO;
import org.mule.samples.voipservice.to.ServiceParamTO;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOMessage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SyncVoipBroker
{

    protected static transient Log logger = LogFactory.getLog(SyncVoipBroker.class);

    public UMOMessage validate(ServiceParamTO serviceParamTO) throws Exception
    {

        logger.info("Inside Method : " + serviceParamTO);

        UMOMessage msg = null;
        List endPoints = null;
        UMOEventContext umoEventContext = RequestContext.getEventContext();
        UMOMessage umoMessage = umoEventContext.sendEvent(serviceParamTO.getCustomer().getAddress());
        Integer isValidAddress = (Integer)umoMessage.getPayload();
        if (isValidAddress.intValue() == AddressValidation.SUCCESS)
        {
            umoMessage = umoEventContext.sendEvent(serviceParamTO.getCreditCard().getCardType());
            endPoints = (List)umoMessage.getPayload();
            logger.info("Inside Method : isValidAddress = " + isValidAddress + "; endPoints = " + endPoints);
            Map props = new HashMap();
            props.put("recipients", endPoints);
            msg = new MuleMessage(new CreditProfileTO(serviceParamTO.getCustomer()), props);
            umoEventContext.dispatchEvent(msg);
            umoEventContext.setStopFurtherProcessing(true);
        }
        return msg;
    }
}
