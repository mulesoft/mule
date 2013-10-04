/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer;

import org.mule.api.transformer.DataType;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.transformer.types.CollectionDataType;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transformer.types.MimeTypes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@SmallTest
public class DataTypesTestCase extends AbstractMuleTestCase
{
    //Just used for testing
    private List<Exception> listOfExceptions;

    @Test
    public void testSimpleTypes() throws Exception
    {
        DataType dt = DataTypeFactory.create(Exception.class);
        DataType dt2 = DataTypeFactory.create(Exception.class);

        assertTrue(dt.isCompatibleWith(dt2));
        assertEquals(dt, dt2);

        dt2 = DataTypeFactory.create(IOException.class);

        assertTrue(dt.isCompatibleWith(dt2));
        assertFalse(dt.equals(dt2));

        //Check mime type matching
        dt2 = DataTypeFactory.create(IOException.class, "application/exception+java");

        //Will match because the default mime type is '*/*'
        assertTrue(dt.isCompatibleWith(dt2));
        assertFalse(dt.equals(dt2));

        dt.setMimeType(MimeTypes.BINARY);

        assertFalse(dt.isCompatibleWith(dt2));
        assertFalse(dt.equals(dt2));

        dt = DataTypeFactory.create(Exception.class);
        dt2 = DataTypeFactory.STRING;

        assertFalse(dt.isCompatibleWith(dt2));
        assertFalse(dt.equals(dt2));
    }

    @Test
    public void testCollectionTypes() throws Exception
    {
        DataType dt = DataTypeFactory.create(List.class);
        DataType dt2 = DataTypeFactory.create(List.class);

        assertTrue(dt.isCompatibleWith(dt2));
        assertEquals(dt, dt2);

        dt2 = DataTypeFactory.create(ArrayList.class);

        assertTrue(dt.isCompatibleWith(dt2));
        assertFalse(dt.equals(dt2));

        //Check mime type matching
        dt2 = DataTypeFactory.create(ArrayList.class, "application/list+java");

        //Will match because the default mime type is '*/*'
        assertTrue(dt.isCompatibleWith(dt2));
        assertFalse(dt.equals(dt2));

        dt.setMimeType(MimeTypes.BINARY);

        assertFalse(dt.isCompatibleWith(dt2));
        assertFalse(dt.equals(dt2));

        dt = DataTypeFactory.create(List.class);
        dt2 = DataTypeFactory.create(Set.class);

        assertFalse(dt.isCompatibleWith(dt2));
        assertFalse(dt.equals(dt2));

    }

    @Test
    public void testGenericCollectionTypes() throws Exception
    {
        DataType dt = DataTypeFactory.create(List.class, Exception.class);
        DataType dt2 = DataTypeFactory.create(List.class, Exception.class);

        assertTrue(dt.isCompatibleWith(dt2));
        assertEquals(dt, dt2);

        dt2 = DataTypeFactory.create(ArrayList.class, IOException.class);

        assertTrue(dt.isCompatibleWith(dt2));
        assertFalse(dt.equals(dt2));

        //Check mime type matching
        dt2 = DataTypeFactory.create(ArrayList.class, IOException.class, "application/list+java");

        //Will match because the default mime type is '*/*'
        assertTrue(dt.isCompatibleWith(dt2));
        assertFalse(dt.equals(dt2));

        dt.setMimeType(MimeTypes.BINARY);

        assertFalse(dt.isCompatibleWith(dt2));
        assertFalse(dt.equals(dt2));

        //Test Generic Item types don't match
        dt = DataTypeFactory.create(List.class, Exception.class);
        dt2 = DataTypeFactory.create(List.class, String.class);

        assertFalse(dt.isCompatibleWith(dt2));
        assertFalse(dt.equals(dt2));
    }


    @Test
    public void testGenericCollectionTypesFromMethodReturn() throws Exception
    {
        DataType dt = DataTypeFactory.createFromReturnType(getClass().getDeclaredMethod("listOfExceptionsMethod", String.class));
        assertTrue(dt instanceof CollectionDataType);

        assertEquals(List.class, dt.getType());
        assertEquals(Exception.class, ((CollectionDataType) dt).getItemType());

        DataType dt2 = DataTypeFactory.createFromReturnType(getClass().getDeclaredMethod("listOfExceptionsMethod", String.class));
        assertTrue(dt.isCompatibleWith(dt2));
        assertEquals(dt, dt2);

        dt2 = DataTypeFactory.createFromReturnType(getClass().getDeclaredMethod("listOfExceptionsMethod", Integer.class));
        assertTrue(dt.isCompatibleWith(dt2));
        assertFalse(dt.equals(dt2));

    }

    @Test
    public void testGenericCollectionTypesFromMethodParam() throws Exception
    {
        DataType dt = DataTypeFactory.createFromParameterType(getClass().getDeclaredMethod("listOfExceptionsMethod", Collection.class), 0);
        assertTrue(dt instanceof CollectionDataType);

        assertEquals(Collection.class, dt.getType());
        assertEquals(Exception.class, ((CollectionDataType) dt).getItemType());

        DataType dt2 = DataTypeFactory.createFromParameterType(getClass().getDeclaredMethod("listOfExceptionsMethod", Collection.class), 0);
        assertTrue(dt.isCompatibleWith(dt2));
        assertEquals(dt, dt2);

        dt2 = DataTypeFactory.createFromParameterType(getClass().getDeclaredMethod("listOfExceptionsMethod", List.class), 0);
        assertTrue(dt.isCompatibleWith(dt2));
        assertFalse(dt.equals(dt2));
    }

    @Test
    public void testGenericCollectionTypesFromField() throws Exception
    {
        DataType dt = DataTypeFactory.createFromField(getClass().getDeclaredField("listOfExceptions"));
        assertTrue(dt instanceof CollectionDataType);

        assertEquals(List.class, dt.getType());
        assertEquals(Exception.class, ((CollectionDataType) dt).getItemType());
    }

    private List<Exception> listOfExceptionsMethod(String s)
    {
        return null;
    }

    private ArrayList<IOException> listOfExceptionsMethod(Integer i)
    {
        return null;
    }

    private String listOfExceptionsMethod(Collection<Exception> exceptions)
    {
        return null;
    }

    private Integer listOfExceptionsMethod(List<IOException> ioExceptions)
    {
        return null;
    }
}
