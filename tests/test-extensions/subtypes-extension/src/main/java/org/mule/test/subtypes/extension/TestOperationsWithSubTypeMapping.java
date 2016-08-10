/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.subtypes.extension;


import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.UseConfig;
import org.mule.test.vegan.extension.VeganCookBook;

import java.util.Arrays;
import java.util.List;

public class TestOperationsWithSubTypeMapping
{

    public ParentShape shapeRetriever(ParentShape shape)
    {
        return shape;
    }

    public Door doorRetriever(Door door)
    {
        return door;
    }

    public SubTypesMappingConnector configRetriever(@UseConfig SubTypesMappingConnector config)
    {
        return config;
    }

    public SubTypesConnectorConnection connectionRetriever(@Connection SubTypesConnectorConnection connection)
    {
        return connection;
    }

    public List<Object> subtypedAndConcreteParameters(ParentShape baseShape, Door door, FinalPojo finalPojo,
                                                      VeganCookBook cookBook, ParentShape squareShape, ParentShape triangle)
    {
        return Arrays.asList(baseShape, door, finalPojo, cookBook, squareShape, triangle);
    }
}
