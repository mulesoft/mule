/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.example.loanbroker;

import org.mule.example.loanbroker.AbstractLoanBrokerApp;
//import org.mule.tck.util.MuleDerbyTestUtils;

/**
 * Executes the LoanBroker BPM example.  This is a standalone app which assumes Mule is running somewhere.
 */
public class LoanBrokerApp extends AbstractLoanBrokerApp
{
    public LoanBrokerApp() throws Exception
    {
        super();
    }    
    
    public static void main(String[] args) throws Exception
    {
        //LoanBrokerApp loanBrokerApp = new LoanBrokerApp(configFile);
        LoanBrokerApp loanBrokerApp = new LoanBrokerApp();
        // FIXME DZ: need to know if we are running sync/async servers
        loanBrokerApp.run(true);
    }

    @Override
    protected void init() throws Exception
    {
        super.init();
    }
}
