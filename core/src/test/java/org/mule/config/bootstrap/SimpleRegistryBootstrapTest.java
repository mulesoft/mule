/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.bootstrap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transaction.TransactionFactory;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

public class SimpleRegistryBootstrapTest extends AbstractMuleContextTestCase
{
    
    public static final String TEST_TRANSACTION_FACTORY_NAME = "jms.singletx.transaction.resource1";
    public static final String TEST_TRANSACTION_FACTORY_CLASS = "javax.jms.Connection";
    
    @Before
    public void initRegistryBootstrap() throws InitialisationException
    {
        SimpleRegistryBootstrap simpleRegistryBootstrap = new SimpleRegistryBootstrap();
        simpleRegistryBootstrap.setMuleContext(muleContext);
        simpleRegistryBootstrap.initialise();
    }
    
    @Test(expected=ClassNotFoundException.class)
    public void testRegisteringOptionalTransaction() throws ClassNotFoundException
    {
        muleContext.getTransactionFactoryManager().getTransactionFactoryFor(Class.forName(TEST_TRANSACTION_FACTORY_CLASS));
    }
    
    @Test
    public void testExistingNotOptionalTransaction() throws Exception
    {
        TransactionFactory transactionFactoryFor = muleContext.getTransactionFactoryManager().getTransactionFactoryFor(FakeTransactionResource.class);
        Assert.assertNotNull(transactionFactoryFor);
        
    }
    
    
}
