/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.example.loanbroker.esn;

import org.mule.example.loanbroker.AbstractLoanBrokerApp;
import org.mule.example.loanbroker.LocaleMessage;
import org.mule.example.loanbroker.messages.LoanQuote;
import org.mule.util.DateUtils;
import org.mule.util.StringMessageUtils;
import org.mule.util.StringUtils;
import org.mule.util.SystemUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Runs the LoanBroker ESN example application.
 */
public class LoanBrokerApp extends AbstractLoanBrokerApp
{
    public static final String CLI_OPTIONS[][] = {
            { "config", "true", "Configuration File(s)" },
            { "main", "true", "LoanBrokerApp Class"},
            { "req", "true", "Number of loan requests to use"},
            { "sync", "true", "Whether to run in synchronous mode or not"}
        };

    private static boolean synchronous = false;

    // Needed for webapp version!
    // TODO Travis ... sadly, it doesn't quite work
    public LoanBrokerApp() throws Exception
    {
        super();
    }

    public LoanBrokerApp(String config) throws Exception
    {
        super(config);
    }

    public static void main(String[] args) throws Exception
    {
        LoanBrokerApp loanBrokerApp = null;
        
        /////////////////////////////////////////
        // Command-line config
        /////////////////////////////////////////
        Map<String, Object> options = SystemUtils.getCommandLineOptions(args, CLI_OPTIONS);
        String config = (String)options.get("config");
        if (StringUtils.isNotBlank(config))
        {
            loanBrokerApp = new LoanBrokerApp(config);

            int i = 100;
            String requests = (String)options.get("req");
            if (requests != null)
            {
                i = Integer.parseInt(requests);
            }

            String sync = (String)options.get("sync");
            if (sync != null)
            {
                synchronous = Boolean.valueOf(sync).booleanValue();
            }

            if (synchronous)
            {
                long start = System.currentTimeMillis();
                List<Object> results = loanBrokerApp.requestSend(i, "CustomerRequests");
                System.out.println(LocaleMessage.responseNumQuotes(results.size()));
                List<String> output = new ArrayList<String>(results.size());
                int x = 1;
                for (Iterator<Object> iterator = results.iterator(); iterator.hasNext(); x++)
                {
                    LoanQuote quote = (LoanQuote) iterator.next();
                    output.add(x + ". " + quote.toString());
                }
                
                System.out.println(StringMessageUtils.getBoilerPlate(output, '*', 80));
                long cur = System.currentTimeMillis();
                System.out.println(DateUtils.getFormattedDuration(cur - start));
                System.out.println(LocaleMessage.responseAvgRequest(((cur - start) / x)));
            }
            else
            {
                loanBrokerApp.requestDispatch(i, "CustomerRequests");
            }
        }
        /////////////////////////////////////////
        // Interactive config
        /////////////////////////////////////////
        else
        {
            loanBrokerApp = new LoanBrokerApp(getInteractiveConfig());
            loanBrokerApp.run(synchronous);
        }
    }

    protected static String getInteractiveConfig() throws IOException
    {
        System.out.println(StringMessageUtils.getBoilerPlate(LocaleMessage.welcome()));
                    
        int response = 0;
        String mode = null;
        while (response != 'a' && response != 's')
        {
            System.out.println("\n" + LocaleMessage.menuOptionMode());
            response = readCharacter();
            switch (response)
            {
                case 'a' :
                {
                    System.out.println(LocaleMessage.loadingAsync());
                    mode = "async";
                    break;
                }

                case 's' :
                {
                    System.out.println(LocaleMessage.loadingSync());
                    mode = "sync";
                    break;
                }
            }
        }

        String config = "loan-broker-" + mode + "-config.xml, loan-broker-cxf-endpoints-config.xml";
        return config;
    }
}
