/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
