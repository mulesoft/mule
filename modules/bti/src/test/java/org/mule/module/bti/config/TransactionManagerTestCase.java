/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.bti.config;

import static org.junit.Assert.assertThat;

import org.mule.module.bti.transaction.TransactionManagerWrapper;
import org.mule.tck.junit4.FunctionalTestCase;

import org.hamcrest.core.IsInstanceOf;
import org.junit.Test;

public class TransactionManagerTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "tm-namespacehandler.xml";
    }

    @Test
    public void testNamespaceHandler()
    {
        assertThat(muleContext.getTransactionManager(), IsInstanceOf.instanceOf(TransactionManagerWrapper.class));
    }

}
