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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.samples.voipservice.interfaces.BankAgency;
import org.mule.samples.voipservice.to.CreditProfileTO;

/**
 * @author Binildas Christudas
 */
public class BankAgencyService implements BankAgency
{

    protected static transient Log logger = LogFactory.getLog(BankAgencyService.class);

    public CreditProfileTO getAuthorisedStatus(CreditProfileTO creditProfileTO)
    {

        logger.info("Inside BankAgencyService.getAuthorisedStatus() ***************");
        creditProfileTO.setCreditAuthorisedStatus(CreditProfileTO.CREDIT_AUTHORISED);
        return creditProfileTO;
    }

}
