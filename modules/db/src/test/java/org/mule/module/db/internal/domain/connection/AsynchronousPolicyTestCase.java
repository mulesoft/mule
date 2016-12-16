/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.connection;

import org.junit.Test;

import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.retry.policies.AbstractPolicyTemplate;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;


import static org.mockito.Mockito.mock;

@SmallTest
public class AsynchronousPolicyTestCase extends AbstractMuleTestCase
{

    private final RetryPolicyTemplate retryPolicyTemplate = mock(AbstractPolicyTemplate.class);
    private final ConnectionFactory delegate = mock(ConnectionFactory.class);

    private RetryConnectionFactory connectionFactory;

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidAsynchronousPolicy()
    {
        connectionFactory = new RetryConnectionFactory(retryPolicyTemplate, delegate);
    }
}
