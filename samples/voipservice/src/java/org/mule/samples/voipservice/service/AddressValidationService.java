package org.mule.samples.voipservice.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.samples.voipservice.interfaces.AddressValidation;
import org.mule.samples.voipservice.to.AddressTO;

public class AddressValidationService implements AddressValidation {

    protected static transient Log logger = LogFactory.getLog(AddressValidationService.class);

    public int validateAddress(AddressTO addressTO) {

        logger.info("Inside AddressValidationService.validateAddress() ***************");
        return AddressValidation.SUCCESS;
    }

}