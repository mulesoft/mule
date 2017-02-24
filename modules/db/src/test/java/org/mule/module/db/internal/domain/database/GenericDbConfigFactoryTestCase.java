/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.database;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.api.config.MuleProperties.OBJECT_DEFAULT_RETRY_POLICY_TEMPLATE;
import static org.mule.module.db.internal.domain.transaction.TransactionalAction.JOIN_IF_POSSIBLE;
import org.mule.api.MuleContext;
import org.mule.api.context.WorkManager;
import org.mule.api.registry.MuleRegistry;
import org.mule.api.retry.RetryCallback;
import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.module.db.internal.domain.connection.DbConnectionFactory;
import org.mule.module.db.internal.domain.type.DbType;
import org.mule.retry.policies.NoRetryPolicyTemplate;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;
import javax.xml.namespace.QName;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;


public class GenericDbConfigFactoryTestCase extends AbstractMuleTestCase
{

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

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
    private DbConnectionFactory dbConnectionFactory = null;

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
        expectedException.expect(SQLException.class);
        when(muleRegistry.lookupObject(OBJECT_DEFAULT_RETRY_POLICY_TEMPLATE)).thenReturn(globalRetryPolicyTemplate);
        dbConfig = genericDbConfigFactory.create("name", annotations, dataSource);
        dbConnectionFactory = dbConfig.getConnectionFactory();
        try
        {
            dbConnectionFactory.createConnection(JOIN_IF_POSSIBLE);
        }
        finally
        {
            verify(globalRetryPolicyTemplate).execute(any(RetryCallback.class), any(WorkManager.class));
        }
    }

    @Test
    public void testOnlyExistsSpecificRetryPolicyTemplate() throws Exception
    {
        expectedException.expect(SQLException.class);
        genericDbConfigFactory.setRetryPolicyTemplate(specificRetryPolicyTemplate);
        dbConfig = genericDbConfigFactory.create("name", annotations, dataSource);
        dbConnectionFactory = dbConfig.getConnectionFactory();
        try
        {
            dbConnectionFactory.createConnection(JOIN_IF_POSSIBLE);
        }
        finally
        {
            verify(specificRetryPolicyTemplate).execute(any(RetryCallback.class), any(WorkManager.class));
        }
    }

    @Test
    public void testNotExistGlobalRetryPolicyTemplate() throws Exception
    {
        when(muleRegistry.lookupObject(OBJECT_DEFAULT_RETRY_POLICY_TEMPLATE)).thenReturn(noRetryPolicyTemplate);
        assertThat(genericDbConfigFactory.getDefaultRetryPolicyTemplate(), is(nullValue()));
    }

    @Test
    public void testExistsBothRetryPolicyTemplates() throws Exception
    {
        expectedException.expect(SQLException.class);
        genericDbConfigFactory.setRetryPolicyTemplate(specificRetryPolicyTemplate);
        when(muleRegistry.lookupObject(OBJECT_DEFAULT_RETRY_POLICY_TEMPLATE)).thenReturn(globalRetryPolicyTemplate);
        dbConfig = genericDbConfigFactory.create("name", annotations, dataSource);
        dbConnectionFactory = dbConfig.getConnectionFactory();
        try
        {
            dbConnectionFactory.createConnection(JOIN_IF_POSSIBLE);
        }
        finally
        {
            verify(specificRetryPolicyTemplate).execute(any(RetryCallback.class), any(WorkManager.class));
        }
    }

}
