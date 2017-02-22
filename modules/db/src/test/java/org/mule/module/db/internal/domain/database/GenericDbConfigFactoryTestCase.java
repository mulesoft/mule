/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.database;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.api.config.MuleProperties.OBJECT_DEFAULT_RETRY_POLICY_TEMPLATE;
import org.mule.api.MuleContext;
import org.mule.api.registry.MuleRegistry;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.module.db.internal.domain.connection.RetryConnectionFactory;
import org.mule.module.db.internal.domain.connection.SimpleConnectionFactory;
import org.mule.module.db.internal.domain.connection.TransactionalDbConnectionFactory;
import org.mule.module.db.internal.domain.type.DbType;
import org.mule.retry.policies.NoRetryPolicyTemplate;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;
import javax.xml.namespace.QName;

import org.junit.Before;
import org.junit.Test;


public class GenericDbConfigFactoryTestCase extends AbstractMuleTestCase
{

    private final GenericDbConfigFactory genericDbConfigFactory = new GenericDbConfigFactory();
    private final Map<QName, Object> annotations = mock(Map.class);
    private final DataSource dataSource = mock(DataSource.class);
    private final List<DbType> customDataTypes = new ArrayList<>();
    private final MuleContext muleContext = mock(MuleContext.class);
    private final MuleRegistry muleRegistry = mock(MuleRegistry.class);
    private final RetryPolicyTemplate globalRetryPolicyTemplate = mock(RetryPolicyTemplate.class);
    private final NoRetryPolicyTemplate noRetryPolicyTemplate = mock(NoRetryPolicyTemplate.class);
    private final RetryPolicyTemplate specificRetryPolicyTemplate = mock(RetryPolicyTemplate.class);
    private DbConfig dbConfig = null;
    private TransactionalDbConnectionFactory transactionalDbConnectionFactory = null;
    private RetryConnectionFactory retryConnectionFactory = null;

    @Before
    public void setUp() throws Exception
    {
        genericDbConfigFactory.setMuleContext(muleContext);
        genericDbConfigFactory.setCustomDataTypes(customDataTypes);
        when(muleContext.getRegistry()).thenReturn(muleRegistry);
        when(globalRetryPolicyTemplate.isSynchronous()).thenReturn(true);
        when(specificRetryPolicyTemplate.isSynchronous()).thenReturn(true);
    }

    @Test
    public void testOnlyExistsGlobalRetryPolicyTemplate() throws Exception
    {
        when(muleRegistry.lookupObject(OBJECT_DEFAULT_RETRY_POLICY_TEMPLATE)).thenReturn(globalRetryPolicyTemplate);
        dbConfig = genericDbConfigFactory.create("name", annotations, dataSource);
        assertThat(dbConfig.getConnectionFactory(), instanceOf(TransactionalDbConnectionFactory.class));
        transactionalDbConnectionFactory = (TransactionalDbConnectionFactory) dbConfig.getConnectionFactory();
        assertThat(transactionalDbConnectionFactory.getConnectionFactory(), instanceOf(RetryConnectionFactory.class));
        retryConnectionFactory = (RetryConnectionFactory) transactionalDbConnectionFactory.getConnectionFactory();
        assertEquals(retryConnectionFactory.getRetryPolicyTemplate(), globalRetryPolicyTemplate);
    }

    @Test
    public void testOnlyExistsSpecificRetryPolicyTemplate() throws Exception
    {
        genericDbConfigFactory.setRetryPolicyTemplate(specificRetryPolicyTemplate);
        dbConfig = genericDbConfigFactory.create("name", annotations, dataSource);
        assertThat(dbConfig.getConnectionFactory(), instanceOf(TransactionalDbConnectionFactory.class));
        transactionalDbConnectionFactory = (TransactionalDbConnectionFactory) dbConfig.getConnectionFactory();
        assertThat(transactionalDbConnectionFactory.getConnectionFactory(), instanceOf(RetryConnectionFactory.class));
        retryConnectionFactory = (RetryConnectionFactory) transactionalDbConnectionFactory.getConnectionFactory();
        assertEquals(retryConnectionFactory.getRetryPolicyTemplate(), specificRetryPolicyTemplate);
    }

    @Test
    public void testNotExistAnyRetryPolicyTemplate() throws Exception
    {
        when(muleRegistry.lookupObject(OBJECT_DEFAULT_RETRY_POLICY_TEMPLATE)).thenReturn(noRetryPolicyTemplate);
        dbConfig = genericDbConfigFactory.create("name", annotations, dataSource);
        assertThat(dbConfig.getConnectionFactory(), instanceOf(TransactionalDbConnectionFactory.class));
        transactionalDbConnectionFactory = (TransactionalDbConnectionFactory) dbConfig.getConnectionFactory();
        assertThat(transactionalDbConnectionFactory.getConnectionFactory(), instanceOf(SimpleConnectionFactory.class));
    }

    @Test
    public void testExistsBothRetryPolicyTemplates() throws Exception
    {
        genericDbConfigFactory.setRetryPolicyTemplate(specificRetryPolicyTemplate);
        when(muleRegistry.lookupObject(OBJECT_DEFAULT_RETRY_POLICY_TEMPLATE)).thenReturn(globalRetryPolicyTemplate);
        dbConfig = genericDbConfigFactory.create("name", annotations, dataSource);
        assertThat(dbConfig.getConnectionFactory(), instanceOf(TransactionalDbConnectionFactory.class));
        transactionalDbConnectionFactory = (TransactionalDbConnectionFactory) dbConfig.getConnectionFactory();
        assertThat(transactionalDbConnectionFactory.getConnectionFactory(), instanceOf(RetryConnectionFactory.class));
        retryConnectionFactory = (RetryConnectionFactory) transactionalDbConnectionFactory.getConnectionFactory();
        assertEquals("Specific Retry Policy should override Global Retry Policy", retryConnectionFactory.getRetryPolicyTemplate(), specificRetryPolicyTemplate);
    }

}
