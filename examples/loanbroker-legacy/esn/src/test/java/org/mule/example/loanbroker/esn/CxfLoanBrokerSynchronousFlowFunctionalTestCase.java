/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.loanbroker.esn;

public class CxfLoanBrokerSynchronousFlowFunctionalTestCase extends CxfLoanBrokerSynchronousFunctionalTestCase
{
    @Override
    protected String[] getConfigFiles()
    {
        return new String[] { "loan-broker-sync-flow-config.xml", "loan-broker-cxf-endpoints-config.xml" };
    }
}
