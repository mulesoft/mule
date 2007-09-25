/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.examples.loanbroker.bpm;

import org.mule.config.ConfigurationBuilder;
import org.mule.config.ConfigurationException;
import org.mule.config.builders.MuleXmlConfigurationBuilder;
import org.mule.config.i18n.MessageFactory;
import org.mule.examples.loanbroker.tests.AbstractAsynchronousLoanBrokerTestCase;
import org.mule.providers.bpm.BPMS;
import org.mule.providers.bpm.ProcessConnector;
import org.mule.util.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.derby.jdbc.EmbeddedDriver;


public class JBpmFunctionalTestCase extends AbstractAsynchronousLoanBrokerTestCase
{
    /** For unit tests, we assume a virgin database, therefore the process ID is assumed to be = 1 */
    public static final long PROCESS_ID = 1;
    
    protected void cleanupDerbyDb(String derbySystemHome) throws IOException, SQLException
    {
        Properties derbyProperties = new Properties();
        derbyProperties.load(new FileInputStream("conf/derby.properties"));
        String derbyDbConnection = derbyProperties.getProperty("database.connection");
        String derbyDbName = derbyProperties.getProperty("database.name");
        derbyDbConnection = derbyDbConnection.replaceAll("\\$\\{database.name\\}", derbyDbName + ";create=true");
        FileUtils.deleteTree(new File(derbySystemHome + "/" + derbyDbName));
        EmbeddedDriver embeddedDriver = new EmbeddedDriver();
        embeddedDriver.connect(derbyDbConnection, null);
    }
    
    protected void suitePreSetUp() throws Exception
    {
        // set the derby.system.home system property to make sure that all derby databases are
        // created in maven's target directory
        File derbySystemHome = new File(System.getProperty("user.dir"), "target");
        System.setProperty("derby.system.home",  derbySystemHome.getAbsolutePath());
        
        cleanupDerbyDb(derbySystemHome.getAbsolutePath());

        super.suitePreSetUp();
    }

    protected ConfigurationBuilder getBuilder() throws Exception 
    {
        return new MuleXmlConfigurationBuilder();
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
            throw new ConfigurationException(MessageFactory.createStaticMessage("Unable to look up jBpmConnector from Mule registry."));
        }
        BPMS bpms = connector.getBpms();
        // TODO MULE-1558 The following assert is throwing a 
        //   org.hibernate.LazyInitializationException: could not initialize proxy - the owning Session was closed
        // See http://forum.springframework.org/archive/index.php/t-24800.html
        //assertEquals("loanApproved", bpms.getState(bpms.lookupProcess(new Long(PROCESS_ID))));
    }
    
    public void testLotsOfLoanRequests() throws Exception
    {
        super.testLotsOfLoanRequests();
        
        //without this sleep, the test still succeeds but throws a series of exceptions
        //probably Spring would not have enough time to close db connections before
        //database itself is shut down while jvm start disposing
        Thread.sleep(100);
    }
}
