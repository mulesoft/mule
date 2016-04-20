/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.exception;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.extension.api.introspection.exception.ExceptionEnricher;
import org.mule.runtime.extension.api.introspection.exception.ExceptionEnricherFactory;
import org.mule.runtime.extension.api.introspection.RuntimeExtensionModel;
import org.mule.runtime.extension.api.introspection.source.RuntimeSourceModel;
import org.mule.test.heisenberg.extension.exception.HeisenbergException;
import org.mule.tck.size.SmallTest;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ExceptionEnricherManagerTestCase
{

    private static final String ERROR_MESSAGE = "ERROR MESSAGE";

    @Mock
    private RuntimeExtensionModel extensionModel;

    @Mock
    private RuntimeSourceModel sourceModel;

    @Mock
    private ExceptionEnricherFactory extensionFactory;

    @Mock
    private ExceptionEnricher extensionEnricher;

    @Mock
    private ExceptionEnricherFactory sourceFactory;

    @Mock
    private ExceptionEnricher sourceEnricher;

    private ExceptionEnricherManager manager;

    @Before
    public void beforeTest()
    {
        when(extensionFactory.createEnricher()).thenReturn(extensionEnricher);
        when(extensionModel.getExceptionEnricherFactory()).thenReturn(Optional.of(extensionFactory));

        when(sourceEnricher.enrichException(any(Exception.class))).thenReturn(new HeisenbergException(ERROR_MESSAGE));
        when(sourceFactory.createEnricher()).thenReturn(sourceEnricher);
        when(sourceModel.getExceptionEnricherFactory()).thenReturn(Optional.of(sourceFactory));

        manager = new ExceptionEnricherManager(extensionModel, sourceModel);
    }

    @Test
    public void process()
    {
        ConnectionException connectionException = new ConnectionException("Connection Error");
        Exception exception = manager.processException(connectionException);
        assertThat(exception, is(not(sameInstance(connectionException))));
        assertThat(exception, is(instanceOf(HeisenbergException.class)));
        assertThat(exception.getMessage(), is(ERROR_MESSAGE));
    }

    @Test
    public void handle()
    {
        Throwable e = new Throwable(new RuntimeException(new ExecutionException(new ConnectionException(ERROR_MESSAGE, new Exception()))));
        Exception resultException = manager.handleException(e);
        assertThat(resultException, is(instanceOf(ConnectionException.class)));
        assertThat(resultException.getMessage(), is(ERROR_MESSAGE));
    }

    @Test
    public void findCorrectEnricher()
    {
        assertThat(manager.getExceptionEnricher(), is(sourceEnricher));

        when(sourceModel.getExceptionEnricherFactory()).thenReturn(Optional.empty());
        ExceptionEnricherManager manager = new ExceptionEnricherManager(extensionModel, sourceModel);
        assertThat(manager.getExceptionEnricher(), is(extensionEnricher));
    }

}
