/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.db.internal.domain.xa;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.api.MuleContext;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class CompositeDataSourceDecoratorTestCase extends AbstractMuleTestCase
{

    public static final String DATA_SOURCE_NAME = "name";
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private MuleContext mockMuleContext;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private DataSourceDecorator mockFirstDataSourceDecorator;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private DataSourceDecorator mockSecondDataSourceDecorator;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private DataSource mockDataSource;

    private CompositeDataSourceDecorator decorator;


    @Before
    public void prepareCompositeDataSourceDecorator()
    {
        decorator = new CompositeDataSourceDecorator();
        when(mockMuleContext.getRegistry().lookupObjects(DataSourceDecorator.class)).thenReturn(getDecoratorMocks());
        decorator.init(mockMuleContext);
    }

    @Test
    public void appliesStopsAfterFirstPositive()
    {
        when(mockFirstDataSourceDecorator.appliesTo(mockDataSource, mockMuleContext)).thenReturn(true);
        decorator.decorate(mockDataSource, DATA_SOURCE_NAME, null, mockMuleContext);
        verify(mockFirstDataSourceDecorator).appliesTo(mockDataSource, mockMuleContext);
        verify(mockSecondDataSourceDecorator, Mockito.never()).appliesTo(mockDataSource, mockMuleContext);
    }

    @Test
    public void ifFirstAppliesIsFalseContinueWithNext()
    {
        when(mockFirstDataSourceDecorator.appliesTo(mockDataSource, mockMuleContext)).thenReturn(false);
        decorator.decorate(mockDataSource, DATA_SOURCE_NAME, null, mockMuleContext);
        verify(mockFirstDataSourceDecorator).appliesTo(mockDataSource, mockMuleContext);
        verify(mockSecondDataSourceDecorator).appliesTo(mockDataSource, mockMuleContext);
    }

    private Collection<DataSourceDecorator> getDecoratorMocks()
    {
        List<DataSourceDecorator> decorators = new ArrayList<DataSourceDecorator>();
        decorators.add(mockSecondDataSourceDecorator);
        decorators.add(mockFirstDataSourceDecorator);
        return decorators;
    }

}
