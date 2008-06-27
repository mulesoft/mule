/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jms.integration;

public class JmsSingleTransactionAlwaysBeginConfigurationTestCase extends AbstractJmsFunctionalTestCase
{

    public static final String JMS_QUEUE_INPUT_CONF_A = "in1";
    public static final String JMS_QUEUE_OUTPUT_CONF_A = "out1";
    public static final String JMS_QUEUE_INPUT_CONF_B = "in2";
    public static final String JMS_QUEUE_OUTPUT_CONF_B = "out2";
    public static final String JMS_QUEUE_INPUT_CONF_C = "in3";
    public static final String JMS_QUEUE_OUTPUT_CONF_C = "out3";

    protected String getConfigResources()
    {
        return "providers/activemq/jms-single-tx-ALWAYS_BEGIN-configuration.xml";
    }

    public void testConfigrationA() throws Exception
    {
        scenarioCommit.setInputQueue(JMS_QUEUE_INPUT_CONF_A);
        scenarioRollback.setInputQueue(JMS_QUEUE_INPUT_CONF_A);
        scenarioNotReceive.setInputQueue(JMS_QUEUE_INPUT_CONF_A);
        scenarioCommit.setOutputQueue(JMS_QUEUE_OUTPUT_CONF_A);
        scenarioRollback.setOutputQueue(JMS_QUEUE_OUTPUT_CONF_A);
        scenarioNotReceive.setOutputQueue(JMS_QUEUE_OUTPUT_CONF_A);

        send(scenarioCommit);
        receive(scenarioRollback);
        receive(scenarioCommit);
        receive(scenarioNotReceive);
    }

    public void testConfigrationB() throws Exception
    {
        scenarioCommit.setInputQueue(JMS_QUEUE_INPUT_CONF_B);
        scenarioRollback.setInputQueue(JMS_QUEUE_INPUT_CONF_B);
        scenarioNotReceive.setInputQueue(JMS_QUEUE_INPUT_CONF_B);
        scenarioCommit.setOutputQueue(JMS_QUEUE_OUTPUT_CONF_B);
        scenarioRollback.setOutputQueue(JMS_QUEUE_OUTPUT_CONF_B);
        scenarioNotReceive.setOutputQueue(JMS_QUEUE_OUTPUT_CONF_B);

        send(scenarioCommit);
        receive(scenarioRollback);
        receive(scenarioCommit);
        receive(scenarioNotReceive);
    }

    /*
    public void testConfigrationC() throws Exception
    {
        scenarioCommit.setInputQueue(JMS_QUEUE_INPUT_CONF_C);
        scenarioRollback.setInputQueue(JMS_QUEUE_INPUT_CONF_C);
        scenarioNotReceive.setInputQueue(JMS_QUEUE_INPUT_CONF_C);
        scenarioCommit.setOutputQueue(JMS_QUEUE_OUTPUT_CONF_C);
        scenarioRollback.setOutputQueue(JMS_QUEUE_OUTPUT_CONF_C);
        scenarioNotReceive.setOutputQueue(JMS_QUEUE_OUTPUT_CONF_C);

        send(scenarioCommit);
        receive(scenarioRollback);
        receive(scenarioCommit);
        receive(scenarioNotReceive);
    }
    */

}


