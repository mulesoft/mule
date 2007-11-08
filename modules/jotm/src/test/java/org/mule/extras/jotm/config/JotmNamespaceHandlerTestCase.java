/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extras.jotm.config;

import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.FunctionalTestCase;

import javax.transaction.TransactionManager;

import junit.framework.Assert;
import org.objectweb.jotm.Current;

public class JotmNamespaceHandlerTestCase extends FunctionalTestCase
{
    public String getConfigResources()
    {
        return "jotm-namespace-config.xml";
    }

    public void testTransactionManager() throws Exception
    {
        TransactionManager transactionManager = AbstractMuleTestCase.managementContext.getTransactionManager();
        logger.debug(transactionManager);
        Assert.assertTrue(transactionManager instanceof Current);
    }

}