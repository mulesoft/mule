package org.mule.samples.voipservice.interfaces;

import org.mule.samples.voipservice.to.CreditProfileTO;

import java.io.IOException;

public interface CreditAgency {

    CreditProfileTO getCreditProfile(CreditProfileTO creditProfileTO) throws IOException;
}