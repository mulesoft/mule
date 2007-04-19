/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.examples.loanbroker.bpm;

import org.mule.config.ConfigurationBuilder;
import org.mule.config.ConfigurationException;
import org.mule.config.i18n.Message;
import org.mule.examples.loanbroker.tests.AbstractAsynchronousLoanBrokerTestCase;
import org.mule.extras.spring.config.SpringConfigurationBuilder;
import org.mule.providers.bpm.BPMS;
import org.mule.providers.bpm.ProcessConnector;


public class JBpmFunctionalTestCase extends AbstractAsynchronousLoanBrokerTestCase
{
    /** For unit tests, we assume a virgin database, therefore the process ID is assumed to be = 1 */
    public static final long PROCESS_ID = 1;
    
    protected ConfigurationBuilder getBuilder() throws Exception {
        return new SpringConfigurationBuilder();
    }
    
    // @Override
    protected String getConfigResources()
    {
        return "loan-broker-bpm-mule-config.xml";
    }

    // @Override
    public void testSingleLoanRequest() throws Exception
    {
        super.testSingleLoanRequest();
        
        ProcessConnector connector =
            (ProcessConnector) managementContext.getRegistry().lookupConnector("jBpmConnector");
        if (connector == null)
        {
            throw new ConfigurationException(Message.createStaticMessage("Unable to look up jBpmConnector from Mule registry."));
        }
        BPMS bpms = connector.getBpms();
        // TODO The following assert is throwing a 
        //   org.hibernate.LazyInitializationException: could not initialize proxy - the owning Session was closed
        // See http://forum.springframework.org/archive/index.php/t-24800.html
        //assertEquals("loanApproved", bpms.getState(bpms.lookupProcess(new Long(PROCESS_ID))));
    }
}
