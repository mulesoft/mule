/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer;

import org.mule.api.transformer.DataType;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.transformer.types.CollectionDataType;
import org.mule.transformer.types.DataTypeFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class DataTypesTestCase extends AbstractMuleTestCase
{
    public void testSimpleTypes() throws Exception
    {
        DataTypeFactory factory = new DataTypeFactory();
        DataType dt = factory.create(Exception.class);
        DataType dt2 = factory.create(Exception.class);

        assertTrue(dt.isCompatibleWith(dt2));
        assertEquals(dt, dt2);

        dt2 = factory.create(IOException.class);

        assertTrue(dt.isCompatibleWith(dt2));
        assertFalse(dt.equals(dt2));

        //Check mime type matching
        dt2 = factory.create(IOException.class, "application/exception+java");

        assertFalse(dt.isCompatibleWith(dt2));
        assertFalse(dt.equals(dt2));

        dt.setMimeType("*/*");

        assertTrue(dt.isCompatibleWith(dt2));
        assertFalse(dt.equals(dt2));

        dt = factory.create(Exception.class);
        dt2 = factory.create(String.class);

        assertFalse(dt.isCompatibleWith(dt2));
        assertFalse(dt.equals(dt2));
    }

    public void testCollectionTypes() throws Exception
    {
        DataTypeFactory factory = new DataTypeFactory();
        DataType dt = factory.create(List.class);
        DataType dt2 = factory.create(List.class);

        assertTrue(dt.isCompatibleWith(dt2));
        assertEquals(dt, dt2);

        dt2 = factory.create(ArrayList.class);

        assertTrue(dt.isCompatibleWith(dt2));
        assertFalse(dt.equals(dt2));

        //Check mime type matching
        dt2 = factory.create(ArrayList.class, "application/list+java");

        assertFalse(dt.isCompatibleWith(dt2));
        assertFalse(dt.equals(dt2));

        dt.setMimeType("*/*");

        assertTrue(dt.isCompatibleWith(dt2));
        assertFalse(dt.equals(dt2));

        dt = factory.create(List.class);
        dt2 = factory.create(Set.class);

        assertFalse(dt.isCompatibleWith(dt2));
        assertFalse(dt.equals(dt2));

    }

    public void testGenericCollectionTypes() throws Exception
    {
        DataTypeFactory factory = new DataTypeFactory();
        DataType dt = factory.create(List.class, Exception.class);
        DataType dt2 = factory.create(List.class, Exception.class);

        assertTrue(dt.isCompatibleWith(dt2));
        assertEquals(dt, dt2);

        dt2 = factory.create(ArrayList.class, IOException.class);

        assertTrue(dt.isCompatibleWith(dt2));
        assertFalse(dt.equals(dt2));

        //Check mime type matching
        dt2 = factory.create(ArrayList.class, IOException.class, "application/list+java");

        assertFalse(dt.isCompatibleWith(dt2));
        assertFalse(dt.equals(dt2));

        dt.setMimeType("*/*");

        assertTrue(dt.isCompatibleWith(dt2));
        assertFalse(dt.equals(dt2));

        //Test Generic Item types don't match
        dt = factory.create(List.class, Exception.class);
        dt2 = factory.create(List.class, String.class);

        assertFalse(dt.isCompatibleWith(dt2));
        assertFalse(dt.equals(dt2));
    }


    public void testGenericCollectionTypesFromMethodReturn() throws Exception
    {
        DataTypeFactory factory = new DataTypeFactory();
        DataType dt = factory.createFromReturnType(getClass().getDeclaredMethod("listOfExceptionsMethod", String.class));
        assertTrue(dt instanceof CollectionDataType);

        assertEquals(List.class, dt.getType());
        assertEquals(Exception.class, ((CollectionDataType) dt).getItemType());

        DataType dt2 = factory.createFromReturnType(getClass().getDeclaredMethod("listOfExceptionsMethod", String.class));
        assertTrue(dt.isCompatibleWith(dt2));
        assertEquals(dt, dt2);

        dt2 = factory.createFromReturnType(getClass().getDeclaredMethod("listOfExceptionsMethod", Integer.class));
        assertTrue(dt.isCompatibleWith(dt2));
        assertFalse(dt.equals(dt2));

    }

    public void testGenericCollectionTypesFromMethodParam() throws Exception
    {
        DataTypeFactory factory = new DataTypeFactory();
        DataType dt = factory.createFromParameterType(getClass().getDeclaredMethod("listOfExceptionsMethod", Collection.class), 0);
        assertTrue(dt instanceof CollectionDataType);

        assertEquals(Collection.class, dt.getType());
        assertEquals(Exception.class, ((CollectionDataType) dt).getItemType());

        DataType dt2 = factory.createFromParameterType(getClass().getDeclaredMethod("listOfExceptionsMethod", Collection.class), 0);
        assertTrue(dt.isCompatibleWith(dt2));
        assertEquals(dt, dt2);

        dt2 = factory.createFromParameterType(getClass().getDeclaredMethod("listOfExceptionsMethod", List.class), 0);
        assertTrue(dt.isCompatibleWith(dt2));
        assertFalse(dt.equals(dt2));

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
