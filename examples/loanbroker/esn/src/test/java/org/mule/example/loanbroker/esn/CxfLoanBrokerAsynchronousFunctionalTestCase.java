/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.example.loanbroker.esn;

import org.mule.example.loanbroker.tests.AbstractAsynchronousLoanBrokerTestCase;


public class CxfLoanBrokerAsynchronousFunctionalTestCase extends AbstractAsynchronousLoanBrokerTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "loan-broker-async-config.xml, loan-broker-cxf-endpoints-config.xml";
    }

    @Override
    protected int getNumberOfRequests()
    {
        // MULE-3016
        return 1;
    }

    @Override
    protected int getWarmUpMessages()
    {
        // MULE-3016
        return 0;
    }

}
