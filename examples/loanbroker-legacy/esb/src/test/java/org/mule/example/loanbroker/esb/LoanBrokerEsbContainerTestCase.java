/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.example.loanbroker.esb;

import org.mule.example.loanbroker.tests.AbstractLoanBrokerTestCase;

public class LoanBrokerEsbContainerTestCase extends AbstractLoanBrokerTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "loan-broker-esb-mule-config-with-ejb-container.xml";
    }
}
