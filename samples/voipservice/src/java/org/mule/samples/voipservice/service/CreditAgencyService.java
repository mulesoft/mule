package org.mule.samples.voipservice.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.samples.voipservice.interfaces.CreditAgency;
import org.mule.samples.voipservice.to.CreditProfileTO;

public class CreditAgencyService implements CreditAgency {

    protected static transient Log logger = LogFactory.getLog(CreditAgencyService.class);

    public CreditProfileTO getCreditProfile(CreditProfileTO creditProfileTO) {

        logger.info("Inside CreditAgencyService.getCreditProfile() ***************");
        creditProfileTO.setCreditScore(1000000);
        return creditProfileTO;
    }

}