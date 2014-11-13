/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jms.config;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import javax.jms.ConnectionFactory;

import org.junit.Test;

@SmallTest
public class CachingConnectionFactoryBeanTestCase extends AbstractMuleTestCase
{

    private final ConnectionFactory connectionFactoryDelegate = mock(ConnectionFactory.class);
    private final CachingConnectionFactoryFactoryBean factoryBean = new CachingConnectionFactoryFactoryBean();

    @Test
    public void returnsTargetConnectionFactory() throws Exception
    {
        factoryBean.setConnectionFactory(connectionFactoryDelegate);
        assertThat(factoryBean.createInstance(), equalTo(connectionFactoryDelegate));
    }

}
