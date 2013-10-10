/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.transport.jms.scripting;

/**
 * Tests that we are able to request jms messages from inside the script when
 * the endpoints are defined using no transactions.
 */
public class JmsRequestFromScriptWithNoTransactionTestCase extends AbstractJmsRequestFromScriptTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/transport/jms/scripting/jms-request-from-script-with-no-transaction-config.xml";
    }
}
