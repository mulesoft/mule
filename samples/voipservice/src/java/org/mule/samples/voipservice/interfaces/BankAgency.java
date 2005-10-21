package org.mule.samples.voipservice.interfaces;

import java.io.IOException;

import org.mule.samples.voipservice.to.CreditProfileTO;

public interface BankAgency {

    CreditProfileTO getAuthorisedStatus(CreditProfileTO creditProfileTO) throws IOException;
}