/*
 * $Id: JmsSingleTransactionAlwaysBeginConfigurationTestCase.java 14304 2009-03-15 11:19:11Z dfeist $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jms.integration;

/**
 * Test all combinations of (inbound) BEGIN_OR_JOIN.  They should all pass.
 */
public class JmsSingleTransactionSingleServiceBeginOrJoinConfigurationTestCase extends
    AbstractJmsSingleTransactionSingleServiceTestCase
{
    protected String getConfigResources()
    {
        return "integration/jms-single-tx-single-service-begin-or-join.xml";
    }
}
