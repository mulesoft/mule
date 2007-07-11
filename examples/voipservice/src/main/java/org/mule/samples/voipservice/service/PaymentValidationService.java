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

import org.mule.samples.voipservice.interfaces.PaymentValidation;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Binildas Christudas
 */
public class PaymentValidationService implements PaymentValidation
{

    protected static transient Log logger = LogFactory.getLog(PaymentValidationService.class);

    public List getCreditVendors(String cardType)
    {
        logger.info("Inside PaymentValidationService.getCreditVendors() ***************");
        List endPoints = new ArrayList();
        endPoints.add(CREDIT_AGENCY_LOOKUP_NAME);
        endPoints.add(BANK_AGENCY_LOOKUP_NAME);
        return endPoints;
    }

}
