/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.samples.voipservice.to;

import java.io.Serializable;

/**
 * @author Binildas Christudas
 */
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