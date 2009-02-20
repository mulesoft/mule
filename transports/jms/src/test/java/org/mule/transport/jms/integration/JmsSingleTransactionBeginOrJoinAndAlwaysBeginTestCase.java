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

import org.junit.Test;

public class JmsSingleTransactionBeginOrJoinAndAlwaysBeginTestCase extends AbstractJmsFunctionalTestCase
{
    public JmsSingleTransactionBeginOrJoinAndAlwaysBeginTestCase(JmsVendorConfiguration config)
    {
        super(config);
    }

    protected String getConfigResources()
    {
        return "integration/jms-single-tx-BEGIN_OR_JOIN_AND_ALWAYS_BEGIN.xml";
    }

    @Test
    public void testSingleTransactionBeginOrJoinAndAlwaysBegin() throws Exception
    {
        send(scenarioCommit);
        receive(scenarioRollback);
        receive(scenarioCommit);
        receive(scenarioNotReceive);
    }
}
