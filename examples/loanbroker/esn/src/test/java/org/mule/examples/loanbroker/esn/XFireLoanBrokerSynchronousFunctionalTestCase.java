/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.examples.loanbroker.esn;

import org.mule.examples.loanbroker.tests.AbstractLoanBrokerTestCase;

public class XFireLoanBrokerSynchronousFunctionalTestCase extends AbstractLoanBrokerTestCase
{
    // @Override
    protected String getConfigResources()
    {
        return "loan-broker-xfire-sync-config.xml";
    }
}
