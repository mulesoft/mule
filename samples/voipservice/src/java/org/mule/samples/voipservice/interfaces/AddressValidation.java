package org.mule.samples.voipservice.interfaces;

import org.mule.samples.voipservice.to.AddressTO;

import java.io.IOException;

public interface AddressValidation {

    int SUCCESS = 1;
    int FAILURE = -1;

    int validateAddress(AddressTO addressTO) throws IOException;
}