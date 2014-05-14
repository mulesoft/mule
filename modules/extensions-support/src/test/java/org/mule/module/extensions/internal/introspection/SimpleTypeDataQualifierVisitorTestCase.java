/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.introspection;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.runners.Parameterized.Parameters;
import org.mule.extensions.introspection.DataQualifier;
import org.mule.extensions.introspection.DataQualifierVisitor;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.lang.ArrayUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@SmallTest
@RunWith(Parameterized.class)
public class SimpleTypeDataQualifierVisitorTestCase extends AbstractMuleTestCase
{

    private static final DataQualifier[] SIMPLE_TYPES = new DataQualifier[] {
            DataQualifier.BOOLEAN,
            DataQualifier.INTEGER,
            DataQualifier.DOUBLE,
            DataQualifier.DECIMAL,
            DataQualifier.STRING,
            DataQualifier.LONG,
            DataQualifier.ENUM};

    @Parameters(name = "isSimpleType({0})")
    public static Collection<Object[]> data()
    {
        Collection<Object[]> types = new ArrayList<>();
        for (DataQualifier qualifier : DataQualifier.values())
        {
            types.add(new Object[] {qualifier});
        }

        return types;
    }

    @Parameterized.Parameter(0)
    public DataQualifier qualifier;

    private DataQualifierVisitor visitor = new SimpleTypeDataQualifierVisitor()
    {
        @Override
        protected void onSimpleType()
        {
            simpleType = true;
        }

        @Override
        protected void defaultOperation()
        {
            complexType = true;
        }
    };

    private boolean simpleType;
    private boolean complexType;

    @Before
    public void before()
    {
        simpleType = false;
        complexType = false;
    }

    @Test
    public void assertSimpleOrNot()
    {
        qualifier.accept(visitor);
        boolean shouldBeSimple = ArrayUtils.contains(SIMPLE_TYPES, qualifier);
        assertThat(simpleType, is(shouldBeSimple));
        assertThat(complexType, is(!shouldBeSimple));
    }
}
