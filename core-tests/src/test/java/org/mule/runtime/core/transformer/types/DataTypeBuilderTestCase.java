/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer.types;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.DataTypeOptionalParamsBuilder;
import org.mule.runtime.core.metadata.CollectionDataType;
import org.mule.runtime.core.metadata.SimpleDataType;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class DataTypeBuilderTestCase extends AbstractMuleTestCase
{

    @Rule
    public ExpectedException expected = ExpectedException.none();

    @Test
    public void buildSimple()
    {
        final DataType<String> dataType = DataType.forJavaType(String.class);
        assertThat(dataType, instanceOf(SimpleDataType.class));
        assertThat(dataType.getType(), is(equalTo(String.class)));
    }

    @Test
    public void buildCollection()
    {
        final DataType<Set> dataType = DataType.forJavaType(Set.class);
        assertThat(dataType, instanceOf(CollectionDataType.class));
        assertThat(dataType.getType(), is(equalTo(Set.class)));
        assertThat(((CollectionDataType) dataType).getItemType(), is(equalTo(Object.class)));
    }

    @Test
    public void buildTypedCollection()
    {
        final DataType<Set> dataType = DataType.builder().collectionType(List.class).itemType(String.class).build();
        assertThat(dataType, instanceOf(CollectionDataType.class));
        assertThat(dataType.getType(), is(equalTo(List.class)));
        assertThat(((CollectionDataType) dataType).getItemType(), is(equalTo(String.class)));
    }

    @Test
    public void templateSimple()
    {
        final DataType<String> template = DataType.builder(String.class).mimeType("text/plain;charset=ASCII").build();
        final DataType dataType = DataType.builder().from(template).build();

        assertThat(dataType, instanceOf(SimpleDataType.class));
        assertThat(dataType.getType(), is(equalTo(String.class)));
        assertThat(dataType.getMimeType(), is("text/plain"));
        assertThat(dataType.getEncoding(), is("ASCII"));
    }

    @Test
    public void templateCollection()
    {
        final DataType<Set> template = DataType.builder(Set.class).mimeType("text/plain;charset=ASCII").build();
        final DataType dataType = DataType.builder().from(template).build();

        assertThat(dataType, instanceOf(CollectionDataType.class));
        assertThat(dataType.getType(), is(equalTo(Set.class)));
        assertThat(((CollectionDataType) dataType).getItemType(), is(equalTo(Object.class)));
        assertThat(dataType.getMimeType(), is("text/plain"));
        assertThat(dataType.getEncoding(), is("ASCII"));
    }

    @Test
    public void templateTypedCollection()
    {
        final DataType<Set> template = DataType.builder()
                                               .collectionType(List.class)
                                               .itemType(String.class)
                                               .mimeType("text/plain;charset=ASCII")
                                               .build();
        final DataType dataType = DataType.builder().from(template).build();

        assertThat(dataType, instanceOf(CollectionDataType.class));
        assertThat(dataType.getType(), is(equalTo(List.class)));
        assertThat(((CollectionDataType) dataType).getItemType(), is(equalTo(String.class)));
    }

    @Test
    public void buildCollectionFails()
    {
        expected.expect(IllegalArgumentException.class);
        final DataTypeOptionalParamsBuilder builder = DataType.builder().collectionType(String.class).itemType(String.class);
    }

    @Test
    public void proxy()
    {
        final Class<?> muleMessageProxy = Proxy.getProxyClass(DataTypeBuilderTestCase.class.getClassLoader(), MuleMessage.class);

        final DataType dataType = DataType.forJavaType(muleMessageProxy);

        assertThat(dataType.getType(), is(equalTo(MuleMessage.class)));
    }

    @Test
    public void mimeTypeWithEncoding()
    {
        final DataType dataType = DataType.builder().mimeType("text/plain;charset=ASCII").build();

        assertThat(dataType.getMimeType(), is("text/plain"));
        assertThat(dataType.getEncoding(), is("ASCII"));
    }

    @Test
    public void invalidMimeType()
    {
        expected.expect(IllegalArgumentException.class);
        final DataType dataType = DataType.builder().mimeType("imInvalid").build();
    }

    @Test
    public void invalidEncoding()
    {
        expected.expect(IllegalArgumentException.class);
        final DataType dataType = DataType.builder().encoding("imInvalid").build();
    }

    @Test
    public void recycleBuilder()
    {
        final DataTypeOptionalParamsBuilder<String> builder = DataType.builder(String.class);
        builder.build();

        expected.expect(IllegalStateException.class);
        builder.build();
    }

    @Test
    public void cachedInstances()
    {
        final DataTypeOptionalParamsBuilder<String> builder1 = DataType.builder(String.class);
        final DataTypeOptionalParamsBuilder<String> builder2 = DataType.builder(String.class);

        assertThat(builder1, equalTo(builder2));
        assertThat(builder1.build(), sameInstance(builder2.build()));
    }
}
