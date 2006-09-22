/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.samples.loanbroker.esb.transformers;

import org.mule.samples.loanbroker.esb.message.LoanQuoteRequest;
import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;

/**
 * Extracts the customer information from the request into an array of arguments
 * used to invoke the Credit Agency Session bean
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class LoanQuoteRequestToCreditProfileArgs extends AbstractTransformer
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -1371055512935227423L;

    public LoanQuoteRequestToCreditProfileArgs() {
        registerSourceType(LoanQuoteRequest.class);
    }

    public Object doTransform(Object src, String encoding) throws TransformerException {

        LoanQuoteRequest request = (LoanQuoteRequest)src;
        Object[] args = new Object[2];
        args[0] = request.getCustomerRequest().getCustomer().getName();
        args[1] = new Integer(request.getCustomerRequest().getCustomer().getSsn());
        return args;
    }
}
