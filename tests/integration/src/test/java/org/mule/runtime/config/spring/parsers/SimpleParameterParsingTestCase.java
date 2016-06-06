/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.parsers;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.config.spring.parsers.beans.SimpleCollectionObject;

import java.util.List;
import java.util.Map;

import org.junit.Test;

public class SimpleParameterParsingTestCase extends FunctionalTestCase
{

    private static final String FIRST_NAME_ATTRIBUTE = "firstname";
    private static final String LAST_NAME_ATTRIBUTE = "lastname";
    private static final String AGE_ATTRIBUTE = "age";

    @Override
    protected String getConfigFile()
    {
        return "org/mule/config/spring/parsers/simpler-parameter-collection-config.xml";
    }

    @Test
    public void onlySimpleParametersInSingleAttribute()
    {
        SimpleCollectionObject simpleCollectionObject = muleContext.getRegistry().get("onlySimpleParametersObject");
        Map<Object, Object> simpleParameters = simpleCollectionObject.getSimpleParameters();
        assertThat(simpleParameters.size(), is(3));
        assertFirstChildParameters(simpleParameters);
    }

    @Test
    public void firstComplexChildUsingWrapper()
    {
        SimpleCollectionObject simpleCollectionObject = muleContext.getRegistry().get("onlyComplexFirstChildParameterObject");
        Map<Object, Object> simpleParameters = simpleCollectionObject.getSimpleParameters();
        assertThat(simpleParameters.size(), is(1));
        assertFirstChildParameters(((SimpleCollectionObject) simpleParameters.get("first-child")).getSimpleParameters());
    }

    @Test
    public void secondComplexChildUsingWrapper()
    {
        SimpleCollectionObject simpleCollectionObject = muleContext.getRegistry().get("onlyComplexSecondChildParameterObject");
        Map<Object, Object> simpleParameters = simpleCollectionObject.getSimpleParameters();
        assertThat(simpleParameters.size(), is(1));
        assertSecondChildParameters(((SimpleCollectionObject) simpleParameters.get("second-child")).getSimpleParameters());
    }

    @Test
    public void complexChildrenListUsingWrapper()
    {
        SimpleCollectionObject simpleCollectionObject = muleContext.getRegistry().get("onlyComplexChildrenListParameterObject");
        Map<Object, Object> simpleParameters = simpleCollectionObject.getSimpleParameters();
        assertThat(simpleParameters.size(), is(1));
        assertOtherChildren(simpleParameters);
    }

    @Test
    public void completeParametersObject()
    {
        SimpleCollectionObject simpleCollectionObject = muleContext.getRegistry().get("completeParametersObject");
        Map<Object, Object> simpleParameters = simpleCollectionObject.getSimpleParameters();
        assertThat(simpleParameters.size(), is(6));
        assertFirstChildParameters(simpleParameters);
        assertFirstChildParameters(((SimpleCollectionObject) simpleParameters.get("first-child")).getSimpleParameters());
        assertSecondChildParameters(((SimpleCollectionObject) simpleParameters.get("second-child")).getSimpleParameters());
        assertOtherChildren(simpleParameters);
    }

    private void assertOtherChildren(Map<Object, Object> simpleParameters)
    {
        List<SimpleCollectionObject> collectionObject = (List<SimpleCollectionObject>) simpleParameters.get("other-children");
        assertFirstChildParameters(collectionObject.get(0).getSimpleParameters());
        assertSecondChildParameters(collectionObject.get(1).getSimpleParameters());
    }

    private void assertFirstChildParameters(Map<Object, Object> simpleParameters)
    {
        assertThat(simpleParameters.get(FIRST_NAME_ATTRIBUTE), is("Pablo"));
        assertThat(simpleParameters.get(LAST_NAME_ATTRIBUTE), is("La Greca"));
        assertThat(simpleParameters.get(AGE_ATTRIBUTE), is("32"));
    }

    private void assertSecondChildParameters(Map<Object, Object> simpleParameters)
    {
        assertThat(simpleParameters.get(FIRST_NAME_ATTRIBUTE), is("Mariano"));
        assertThat(simpleParameters.get(LAST_NAME_ATTRIBUTE), is("Gonzalez"));
        assertThat(simpleParameters.get(AGE_ATTRIBUTE), is("31"));
    }
}
