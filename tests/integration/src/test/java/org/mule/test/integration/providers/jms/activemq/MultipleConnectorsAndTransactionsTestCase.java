/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.providers.jms.activemq;

import org.mule.tck.FunctionalTestCase;

public class MultipleConnectorsAndTransactionsTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/test/integration/providers/jms/multiple-connectors-and-transactions-config.xml";
    }

    public void testDispatchingToSeparateEndpoints() throws Exception
    {
        // TODO RM: something missing here?
    }

}
