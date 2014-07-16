/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.db.internal.domain.xa;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleContext;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class DefaultDataSourceDecoratorTestCase extends AbstractMuleTestCase
{

    public static final String DATA_SOURCE_NAME = "name";
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private DataSource mockDataSource;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private DataSourceWrapper mockDataSourceWrapper;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS, extraInterfaces = XADataSource.class)
    private DataSourceWrapper mockXaDataSourceWrapper;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS, extraInterfaces = XADataSource.class)
    private DataSource mockXaDataSource;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MuleContext mockMuleContext;

    private DefaultDataSourceDecorator decorator = new DefaultDataSourceDecorator();

    @Test
    public void appliesToDataSource()
    {
        assertThat(decorator.appliesTo(mockDataSource, mockMuleContext), is(false));
    }

    @Test
    public void appliesToXaDataSource()
    {
        assertThat(decorator.appliesTo(mockXaDataSource, mockMuleContext), is(true));
    }

    @Test
    public void appliesToDataSourceWrapper()
    {
        assertThat(decorator.appliesTo(mockDataSource, mockMuleContext), is(false));
    }

    @Test
    public void appliesToXaDataSourceWrapper()
    {
        assertThat(decorator.appliesTo(mockXaDataSourceWrapper, mockMuleContext), is(false));
    }

    @Test(expected = IllegalStateException.class)
    public void dcorateDataSource()
    {
        decorator.decorate(mockDataSource, DATA_SOURCE_NAME, null, mockMuleContext);
    }

    @Test
    public void decorateXaDataSource()
    {
        decorator.decorate(mockXaDataSource, DATA_SOURCE_NAME, null, mockMuleContext);
    }

    @Test(expected = IllegalStateException.class)
    public void decorateDataSourceWrapper()
    {
        decorator.decorate(mockDataSource, DATA_SOURCE_NAME, null, mockMuleContext);
    }

    @Test(expected = IllegalStateException.class)
    public void decorateXaDataSourceWrapper()
    {
        decorator.decorate(mockXaDataSourceWrapper, DATA_SOURCE_NAME, null, mockMuleContext);
    }
}
