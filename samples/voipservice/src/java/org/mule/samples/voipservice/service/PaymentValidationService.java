package org.mule.samples.voipservice.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleManager;
import org.mule.samples.voipservice.interfaces.PaymentValidation;

import java.util.ArrayList;
import java.util.List;

public class PaymentValidationService implements PaymentValidation {

    protected static transient Log logger = LogFactory.getLog(PaymentValidationService.class);

    public List getCreditVendors(String cardType) {

        logger.info("Inside PaymentValidationService.getCreditVendors() ***************");
        List endPoints = new ArrayList();
        endPoints.add(MuleManager.getInstance().lookupEndpointIdentifier(CREDIT_AGENCY_LOOKUP_NAME, null));
        endPoints.add(MuleManager.getInstance().lookupEndpointIdentifier(BANK_AGENCY_LOOKUP_NAME, null));
        return endPoints;
    }

}