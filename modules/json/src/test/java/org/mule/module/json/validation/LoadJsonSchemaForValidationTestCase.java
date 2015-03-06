/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json.validation;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.module.json.validation.JsonSchemaTestUtils.SCHEMA_FSTAB_JSON;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.io.InputStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class LoadJsonSchemaForValidationTestCase extends AbstractMuleTestCase
{

    @Mock
    private ClassLoader mockClassLoader;

    private InputStream schemaInputStream;

    @Before
    public void before() throws Exception
    {
        schemaInputStream = getClass().getResourceAsStream(SCHEMA_FSTAB_JSON);
        assertThat("could not load schema", schemaInputStream, is(notNullValue()));
        schemaInputStream = spy(schemaInputStream);

        when(mockClassLoader.getResourceAsStream(SCHEMA_FSTAB_JSON)).thenReturn(schemaInputStream);
    }

    @Test
    public void usesThreadClassloader() throws Exception
    {
        doWithMockClasssLoader(new Runnable()
        {
            @Override
            public void run()
            {
                JsonSchemaValidator.builder()
                        .setSchemaLocation(SCHEMA_FSTAB_JSON)
                        .build();
            }
        });

        verify(mockClassLoader).getResourceAsStream(SCHEMA_FSTAB_JSON);
        verify(schemaInputStream, atLeastOnce()).close();
    }

    private void doWithMockClasssLoader(Runnable closure)
    {
        final ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try
        {
            Thread.currentThread().setContextClassLoader(mockClassLoader);
            closure.run();
        }
        finally
        {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }
}
