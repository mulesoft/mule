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
package org.mule.samples.loanbroker.esb;

import org.mule.components.rest.RestServiceWrapper;
import org.mule.samples.loanbroker.esb.message.LoanQuoteRequest;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOMessage;

/**
 * Todo Document class
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class LenderGateway extends RestServiceWrapper {

    public Object onCall(UMOEventContext eventContext) throws Exception
    {
        LoanQuoteRequest request = (LoanQuoteRequest)eventContext.getTransformedMessage();
        UMOMessage result = (UMOMessage)super.onCall(eventContext);
    }
}
