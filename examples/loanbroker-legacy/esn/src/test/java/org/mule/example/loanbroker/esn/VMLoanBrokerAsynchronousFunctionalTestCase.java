/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.loanbroker.esn;

import org.mule.example.loanbroker.tests.AbstractAsynchronousLoanBrokerTestCase;

public class VMLoanBrokerAsynchronousFunctionalTestCase extends AbstractAsynchronousLoanBrokerTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "loan-broker-async-config.xml, loan-broker-vm-endpoints-config.xml";
    }
    
    @Override
    protected int getNumberOfRequests()
    {
        return 100;
    }
}
