package org.mule.samples.voipservice.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.samples.voipservice.interfaces.BankAgency;
import org.mule.samples.voipservice.to.CreditProfileTO;

public class BankAgencyService implements BankAgency {

    protected static transient Log logger = LogFactory.getLog(BankAgencyService.class);

    public CreditProfileTO getAuthorisedStatus(CreditProfileTO creditProfileTO) {

        logger.info("Inside BankAgencyService.getAuthorisedStatus() ***************");
        creditProfileTO.setCreditAuthorisedStatus(CreditProfileTO.CREDIT_AUTHORISED);
        return creditProfileTO;
    }

}