/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.samples.voipservice.to;

import org.mule.samples.voipservice.LocaleMessage;

import java.io.Serializable;

public class ServiceParamTO implements Serializable
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -4807303917627749519L;

    private CustomerTO customerTO;
    private CreditCardTO creditCardTO;

    public ServiceParamTO()
    {
        super();
    }

    public ServiceParamTO(CustomerTO customerTO, CreditCardTO creditCardTO)
    {
        this.customerTO = customerTO;
        this.creditCardTO = creditCardTO;
    }

    public void setCustomer(CustomerTO customerTO)
    {
        this.customerTO = customerTO;
    }

    public CustomerTO getCustomer()
    {
        return customerTO;
    }

    public void setCreditCard(CreditCardTO creditCardTO)
    {
        this.creditCardTO = creditCardTO;
    }

    public CreditCardTO getCreditCard()
    {
        return creditCardTO;
    }

    public String toString()
    {
        StringBuffer stringBuffer = new StringBuffer();
        if (this.customerTO != null)
        {
            stringBuffer.append(LocaleMessage.getCustomerCaption(customerTO));
        }
        if (this.creditCardTO != null)
        {
            stringBuffer.append(LocaleMessage.getCardCaption(creditCardTO));
        }

        return stringBuffer.toString();
    }

}
