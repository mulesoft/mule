package org.mule.samples.voipservice.interfaces;

import java.io.IOException;
import java.util.List;

public interface PaymentValidation {

    int SUCCESS = 1;
    int FAILURE = -1;
    String CREDIT_AGENCY_LOOKUP_NAME = "CreditAgency";
    String BANK_AGENCY_LOOKUP_NAME = "BankAgency";

    List getCreditVendors(String cardType) throws IOException;

}