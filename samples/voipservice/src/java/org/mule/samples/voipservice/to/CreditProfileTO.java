package org.mule.samples.voipservice.to;

import java.io.Serializable;

public class CreditProfileTO implements Serializable {

    public static final int CREDIT_NOT_AUTHORISED = -1;
    public static final int CREDIT_AUTHORISED = 1;
    public static final int CREDIT_LIMIT = 1000;

    private CustomerTO customerTO;
    private int creditScore;
    private int creditAuthorisedStatus;
    private boolean valid;

    public CreditProfileTO() {
    }

    public CreditProfileTO(CustomerTO customerTO) {
        this.customerTO = customerTO;

    }

    public CustomerTO getCustomer() {
        return customerTO;
    }

    public void setCustomer(CustomerTO customerTO) {
        this.customerTO = customerTO;
    }

    public int getCreditScore() {
        return creditScore;
    }

    public void setCreditScore(int creditScore) {
        this.creditScore = creditScore;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public int getCreditAuthorisedStatus() {
        return creditAuthorisedStatus;
    }

    public void setCreditAuthorisedStatus(int creditAuthorisedStatus) {
        this.creditAuthorisedStatus = creditAuthorisedStatus;
    }

}
