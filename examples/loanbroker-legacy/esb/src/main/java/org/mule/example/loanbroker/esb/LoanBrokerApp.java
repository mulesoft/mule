/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.loanbroker.esb;

import org.mule.example.loanbroker.AbstractLoanBrokerApp;
import org.mule.example.loanbroker.LocaleMessage;
import org.mule.util.StringUtils;

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
        String config = getInteractiveConfig();
        if (StringUtils.isNotEmpty(config))
        {
            LoanBrokerApp loanBrokerApp = new LoanBrokerApp(config);
            loanBrokerApp.run(false);
        }
    }

    protected static String getInteractiveConfig() throws IOException
    {
        int response = 0;
        
        System.out.println("******************\n"
            + LocaleMessage.esbWelcome()
            + "\n******************");
        
        while (response != 'q')
        {
            response = readCharacter();
            if (response == '1')
            {
                System.out.println(LocaleMessage.loadingEndpointEjb());
                return "loan-broker-esb-mule-config.xml";
            }
            else
            {
                System.out.println(LocaleMessage.loadingManagedEjb());
                return "loan-broker-esb-mule-config-with-ejb-container.xml";
            }
        }
        
        return "";
    }
}
