/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.example.loanbroker.esb;

import org.mule.api.config.ConfigurationBuilder;
import org.mule.example.loanbroker.tests.AbstractLoanBrokerTestCase;

public class LoanBrokerEsbProgramaticTestCase extends AbstractLoanBrokerTestCase
{
    protected String getConfigResources()
    {
        return "loan-broker-esb-mule-config.xml";
    }

    @Override
    protected ConfigurationBuilder getBuilder() throws Exception
    {
        // TODO Auto-generated method stub
        return new LoanBrokerEsbConfigutationBuilder();
    }
}
