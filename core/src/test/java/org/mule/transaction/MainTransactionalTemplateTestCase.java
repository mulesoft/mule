/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transaction;

import org.hamcrest.core.Is;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.mule.api.MessagingException;
import org.mule.api.transaction.TransactionConfig;
import org.mule.exception.AlreadyHandledMessagingException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.mule.transaction.TransactionTemplateFactory.createMainTransactionTemplate;

@RunWith(MockitoJUnitRunner.class)
public class MainTransactionalTemplateTestCase extends NestedTransactionTemplateTestCase
{

    @Test
    public void testUnwrapAlreadyHandledMessagingException() throws Exception
    {
        MuleTransactionConfig config = new MuleTransactionConfig(TransactionConfig.ACTION_INDIFFERENT);
        TransactionTemplate transactionTemplate = createTransactionalTemplate(config);
        AlreadyHandledMessagingException mockAlreadyManagedException = Mockito.mock(AlreadyHandledMessagingException.class);
        when(mockAlreadyManagedException.getCause()).thenReturn(mockMessagingException);
        try
        {
            transactionTemplate.execute(TransactionTemplateTestUtils.getFailureTransactionCallback(mockAlreadyManagedException));
        }
        catch (MessagingException e)
        {
            assertThat(e, is(mockMessagingException));
        }
    }

    @Override
    @Test
    public void testValidateNullTransactionConfig() {}

    @Override
    @Test
    public void testAlreadyHandledExceptionIsCatch() throws Exception
    {
        try
        {
            when(mockAlreadyHandledMessagingException.getCause()).thenReturn(mockMessagingException);
            super.testAlreadyHandledExceptionIsCatch();
        }
        catch (MessagingException e)
        {
            assertThat(e, Is.is(mockMessagingException));
        }

    }

    @Override
    protected TransactionTemplate createTransactionalTemplate(MuleTransactionConfig config)
    {
        return createMainTransactionTemplate(config, mockMuleContext);
    }
}
