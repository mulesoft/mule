/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.database;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.expression.ExpressionManager;
import org.mule.module.db.internal.domain.connection.DbPoolingProfile;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class DataSourceConfigTestCase extends AbstractMuleTestCase
{

    public static final String MULE_EXPRESSION = "#[expression]";
    public static final String RESOLVED_EXPRESSION = "resolved";

    private final DataSourceConfig dataSourceConfig = new DataSourceConfig();

    @Test
    public void resolvesConfig() throws Exception
    {
        MuleContext context = mock(MuleContext.class);
        ExpressionManager expressionManager = mock(ExpressionManager.class);
        when(context.getExpressionManager()).thenReturn(expressionManager);

        MuleEvent muleEvent = mock(MuleEvent.class);

        final DbPoolingProfile poolingProfile = mock(DbPoolingProfile.class);
        final String url = "url";
        final String password = "password";
        final String user = "user";
        final int connectionTimeout = 10;
        final String driverClassName = "driverClassName";
        final int transactionIsolation = 1;
        final boolean useXaTransactions = true;

        dataSourceConfig.setMuleContext(context);
        dataSourceConfig.setPoolingProfile(poolingProfile);
        dataSourceConfig.setUrl(url);
        dataSourceConfig.setPassword(password);
        dataSourceConfig.setUser(user);
        dataSourceConfig.setConnectionTimeout(connectionTimeout);
        dataSourceConfig.setDriverClassName(driverClassName);
        dataSourceConfig.setTransactionIsolation(transactionIsolation);
        dataSourceConfig.setUseXaTransactions(useXaTransactions);

        DataSourceConfig resolvedDataSourceConfig = dataSourceConfig.resolve(muleEvent);

        assertThat(resolvedDataSourceConfig.getUrl(), equalTo(url));
        assertThat(resolvedDataSourceConfig.getPassword(), equalTo(password));
        assertThat(resolvedDataSourceConfig.getUser(), equalTo(user));
        assertThat(resolvedDataSourceConfig.getConnectionTimeout(), equalTo(connectionTimeout));
        assertThat(resolvedDataSourceConfig.getDriverClassName(), equalTo(driverClassName));
        assertThat(resolvedDataSourceConfig.getTransactionIsolation(), equalTo(transactionIsolation));
        assertThat(resolvedDataSourceConfig.isUseXaTransactions(), equalTo(useXaTransactions));
    }

    @Test
    public void detectsDynamicUrl() throws Exception
    {
        mockDynamicDataSourceConfigDetection();
        dataSourceConfig.setUrl(MULE_EXPRESSION);

        assertThat(dataSourceConfig.isDynamic(), is(true));
    }

    @Test
    public void detectsDynamicDriverClassName() throws Exception
    {
        mockDynamicDataSourceConfigDetection();
        dataSourceConfig.setDriverClassName(MULE_EXPRESSION);

        assertThat(dataSourceConfig.isDynamic(), is(true));
    }

    @Test
    public void detectsDynamicUser() throws Exception
    {
        mockDynamicDataSourceConfigDetection();
        dataSourceConfig.setUser(MULE_EXPRESSION);

        assertThat(dataSourceConfig.isDynamic(), is(true));
    }

    @Test
    public void detectsDynamicPassword() throws Exception
    {
        mockDynamicDataSourceConfigDetection();
        dataSourceConfig.setUser(MULE_EXPRESSION);

        assertThat(dataSourceConfig.isDynamic(), is(true));
    }

    @Test
    public void resolvesDynamicUrl() throws Exception
    {
        MuleEvent muleEvent = mock(MuleEvent.class);
        mockDynamicDataSourceConfigEvaluation(muleEvent);

        DataSourceConfig resolvedDataSourceConfig = dataSourceConfig.resolve(muleEvent);
        assertThat(resolvedDataSourceConfig.getUrl(), is(RESOLVED_EXPRESSION));
    }

    @Test
    public void resolvesDynamicDriverClassName() throws Exception
    {
        MuleEvent muleEvent = mock(MuleEvent.class);
        mockDynamicDataSourceConfigEvaluation(muleEvent);
        dataSourceConfig.setDriverClassName(MULE_EXPRESSION);

        DataSourceConfig resolvedDataSourceConfig = dataSourceConfig.resolve(muleEvent);
        assertThat(resolvedDataSourceConfig.getDriverClassName(), is(RESOLVED_EXPRESSION));
    }

    @Test
    public void resolvesDynamicUser() throws Exception
    {
        MuleEvent muleEvent = mock(MuleEvent.class);
        mockDynamicDataSourceConfigEvaluation(muleEvent);
        dataSourceConfig.setUser(MULE_EXPRESSION);

        DataSourceConfig resolvedDataSourceConfig = dataSourceConfig.resolve(muleEvent);
        assertThat(resolvedDataSourceConfig.getUser(), is(RESOLVED_EXPRESSION));
    }

    @Test
    public void resolvesDynamicPassword() throws Exception
    {
        MuleEvent muleEvent = mock(MuleEvent.class);
            mockDynamicDataSourceConfigEvaluation(muleEvent);
        dataSourceConfig.setPassword(MULE_EXPRESSION);

        DataSourceConfig resolvedDataSourceConfig = dataSourceConfig.resolve(muleEvent);
        assertThat(resolvedDataSourceConfig.getPassword(), is(RESOLVED_EXPRESSION));
    }

    private void mockDynamicDataSourceConfigDetection()
    {
        MuleContext context = mock(MuleContext.class);
        ExpressionManager expressionManager = mock(ExpressionManager.class);
        when(context.getExpressionManager()).thenReturn(expressionManager);
        when(expressionManager.isValidExpression(MULE_EXPRESSION)).thenReturn(true);
        dataSourceConfig.setMuleContext(context);
    }

    private void mockDynamicDataSourceConfigEvaluation(MuleEvent muleEvent)
    {
        ExpressionManager expressionManager = mock(ExpressionManager.class);
        MuleContext context = mock(MuleContext.class);
        when(context.getExpressionManager()).thenReturn(expressionManager);
        when(expressionManager.isValidExpression(MULE_EXPRESSION)).thenReturn(true);
        when(expressionManager.parse(MULE_EXPRESSION, muleEvent)).thenReturn(RESOLVED_EXPRESSION);
        dataSourceConfig.setMuleContext(context);
        dataSourceConfig.setUrl(MULE_EXPRESSION);
    }
}