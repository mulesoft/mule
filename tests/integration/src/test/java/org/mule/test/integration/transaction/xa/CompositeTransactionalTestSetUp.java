/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transaction.xa;

public class CompositeTransactionalTestSetUp implements TransactionalTestSetUp
{
    private final TransactionalTestSetUp[] testSetUps;

    public CompositeTransactionalTestSetUp(TransactionalTestSetUp... testSetUps)
    {
        this.testSetUps = testSetUps;
    }

    @Override
    public void initialize() throws Exception
    {
        for (TransactionalTestSetUp setUp : testSetUps)
        {
            setUp.initialize();
        }
    }

    @Override
    public void finalice() throws Exception
    {
        for (TransactionalTestSetUp setUp : testSetUps)
        {
            setUp.finalice();
        }
    }
}
