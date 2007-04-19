/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.examples.loanbroker.esb;

import org.mule.examples.loanbroker.AbstractLoanBrokerApp;
import org.mule.examples.loanbroker.LocaleMessage;

import java.io.IOException;

/**
 * Runs the LoanBroker ESB example application.
 */

public class LoanBrokerApp extends AbstractLoanBrokerApp
{
    public LoanBrokerApp(String config) throws Exception
    {
        super(config);
    }

    public static void main(String[] args) throws Exception
    {
        LoanBrokerApp loanBrokerApp = null;
        loanBrokerApp = new LoanBrokerApp(getInteractiveConfig());
        loanBrokerApp.run(false);
    }

    protected static String getInteractiveConfig() throws IOException
    {
        int response = 0;
        
        System.out.println("******************\n"
            + LocaleMessage.getString(LocaleMessage.ESB_WELCOME)
            + "\n******************");
        response = readCharacter();
        if (response == '1')
        {
            System.out.println(LocaleMessage.getString(LocaleMessage.LOADING_ENDPOINT_EJB));
            return "loan-broker-esb-mule-config.xml";
        }
        else
        {
            System.out.println(LocaleMessage.getString(LocaleMessage.LOADING_MANAGED_EJB));
            return "loan-broker-esb-mule-config-with-ejb-container.xml";
        }
    }
}
