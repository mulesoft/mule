/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
