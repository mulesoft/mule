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
*
*/
package org.mule.samples.loanbroker.esb.transformers;

import org.mule.samples.loanbroker.esb.message.Customer;
import org.mule.samples.loanbroker.esb.message.CustomerQuoteRequest;
import org.mule.transformers.AbstractEventAwareTransformer;
import org.mule.umo.UMOEventContext;
import org.mule.umo.transformer.TransformerException;

/**
 * Converts parameters on the message into a CustomerQuoteRequest object
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class RestRequestToCustomerRequest extends AbstractEventAwareTransformer {

    public RestRequestToCustomerRequest() {
        setReturnClass(CustomerQuoteRequest.class);
    }

    public Object transform(Object src, UMOEventContext context) throws TransformerException {

        String name = null;
        int ssn = 0;
        double amount = 0;
        int duration = 0;
        try {
            name = getParam(context, "customerName");
            ssn = Integer.valueOf(getParam(context, "ssn")).intValue();
            amount = Double.valueOf(getParam(context, "loanAmount")).doubleValue();
            duration = Integer.valueOf(getParam(context, "loanDuration")).intValue();
        } catch (Exception e) {
            throw new TransformerException(this, e);
        }

        Customer c = new Customer(name, ssn);
        CustomerQuoteRequest request = new CustomerQuoteRequest(c, amount, duration);
        return request;
    }

    protected String getParam(UMOEventContext context, String name) throws NullPointerException
    {
        String value = context.getStringProperty(name);
        if(value==null) {
            throw new NullPointerException("Parameter '" + name + "' must be set on the request");
        }
        return value;
    }
}
