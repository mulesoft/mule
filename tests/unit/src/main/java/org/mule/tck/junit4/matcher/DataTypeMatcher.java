/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.junit4.matcher;

import org.mule.api.transformer.DataType;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class DataTypeMatcher extends TypeSafeMatcher<DataType>
{

    private final Class type;
    private final String mimeType;
    private final String encoding;

    public DataTypeMatcher(Class type, String mimeType, String encoding)
    {
        this.type = type;
        this.mimeType = mimeType;
        this.encoding = encoding;
    }

    @Override
    protected boolean matchesSafely(DataType dataType)
    {
        boolean sameType = type == null ? dataType.getType() == null : type.equals(dataType.getType());
        boolean sameEncoding = encoding == null ? dataType.getEncoding() == null : encoding.equals(dataType.getEncoding());
        boolean sameMimeType = mimeType == null ? dataType.getMimeType() == null : mimeType.equals(dataType.getMimeType());

        return sameType && sameEncoding && sameMimeType;
    }

    @Override
    public void describeTo(Description description)
    {
        description.appendText("a dataType with type = " + type.getName() + ", mimeType= " + mimeType + ", encoding=" + encoding);
    }

    public static Matcher<DataType> like(Class type, String mimeType, String encoding)
    {
        return new DataTypeMatcher(type, mimeType, encoding);
    }

    public static Matcher<DataType> like(DataType dataType)
    {
        return new DataTypeMatcher(dataType.getType(), dataType.getMimeType(), dataType.getEncoding());
    }
}
