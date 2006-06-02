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

import org.mule.samples.loanbroker.esb.message.LoanQuoteRequest;
import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;

/**
 * Extracts the customer information from the request
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class LoanQuoteRequestToCustomer extends AbstractTransformer {

    public LoanQuoteRequestToCustomer() {
        registerSourceType(LoanQuoteRequest.class);
    }

    public Object doTransform(Object src, String encoding) throws TransformerException {

        return ((LoanQuoteRequest)src).getCustomerRequest().getCustomer();
    }
}
