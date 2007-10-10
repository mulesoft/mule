/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.jms.integration;

/**
 * BEGIN_OR_JOIN is same action as ALWAYS_BEGIN for current impl JmsTransaction
 */
public class JmsSingleTransactionBeginOrJoinAndAlwaysBeginTestCase extends AbstractJmsFunctionalTestCase
{
    protected String getConfigResources()
    {
        return "providers/activemq/jms-single-tx-BEGIN_OR_JOIN_AND_ALWAYS_BEGIN.xml";
    }

    public void testSingleTransactionBeginOrJoinAndAlwaysBegin() throws Exception
    {
        send(scenarioCommit);
        receive(scenarioRollback);
        receive(scenarioCommit);
        receive(scenarioNotReceive);
    }
}
