package org.mule.samples.voipservice.to;

import java.io.Serializable;

public class ServiceParamTO implements Serializable {

    private CustomerTO customerTO;
    private CreditCardTO creditCardTO;

    public ServiceParamTO() {
    }

    public ServiceParamTO(CustomerTO customerTO, CreditCardTO creditCardTO) {
        this.customerTO = customerTO;
        this.creditCardTO = creditCardTO;
    }

    public void setCustomer(CustomerTO customerTO) {
        this.customerTO = customerTO;
    }

    public CustomerTO getCustomer() {
        return customerTO;
    }

    public void setCreditCard(CreditCardTO creditCardTO) {
        this.creditCardTO = creditCardTO;
    }

    public CreditCardTO getCreditCard() {
        return creditCardTO;
    }

    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        if (this.customerTO != null) {
            stringBuffer.append("Customer -> " + customerTO);
        }
        if (this.creditCardTO != null) {
            stringBuffer.append("CreditCard -> " + creditCardTO);
        }

        return stringBuffer.toString();
    }

}