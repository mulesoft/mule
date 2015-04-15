/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.fruit.Apple;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Qualifier;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.core.MethodParameter;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class Jsr330AutowireCandidateFilterTestCase extends AbstractMuleTestCase
{

    private static final String NAMED_FIELD = "namedField";

    private static final String DISCARDED_KEY = "discardedKey";
    private static final Object DISCARDED_VALUE = new Object();
    private static final Object WANTED_VALUE = new QualifiedApple();


    private Jsr330AutowireCandidateFilter filter;

    @Mock
    private DependencyDescriptor dependencyDescriptor;

    private Map<String, Object> candidates;

    @Inject
    @Named(NAMED_FIELD)
    private Object namedField;

    @Inject
    @TestAppleQualifier
    private Object qualifiedField;

    @Inject
    @Named(NAMED_FIELD)
    public void setNamedField(Object namedField)
    {
        this.namedField = namedField;
    }

    @Inject
    @TestAppleQualifier
    public void setQualifiedField(Object qualifiedField)
    {
        this.qualifiedField = qualifiedField;
    }

    @Before
    public void before()
    {
        filter = new Jsr330AutowireCandidateFilter();

        candidates = new HashMap<>();
        candidates.put(DISCARDED_KEY, DISCARDED_VALUE);
        candidates.put(NAMED_FIELD, WANTED_VALUE);
    }

    @Test
    public void filterByNamedField() throws Exception
    {
        when(dependencyDescriptor.getField()).thenReturn(getClass().getDeclaredField("namedField"));
        assertFilter();
    }

    @Test
    public void filterByQualifiedField() throws Exception
    {
        when(dependencyDescriptor.getField()).thenReturn(getClass().getDeclaredField("qualifiedField"));
        assertFilter();
    }

    @Test
    public void filterByNamedSetter() throws Exception
    {
        MethodParameter methodParameter = mock(MethodParameter.class);
        when(methodParameter.getMethod()).thenReturn(getClass().getDeclaredMethod("setNamedField", Object.class));
        when(dependencyDescriptor.getMethodParameter()).thenReturn(methodParameter);
        assertFilter();
    }

    @Test
    public void filterByQualifiedSetter() throws Exception
    {
        MethodParameter methodParameter = mock(MethodParameter.class);
        when(methodParameter.getMethod()).thenReturn(getClass().getDeclaredMethod("setQualifiedField", Object.class));
        when(dependencyDescriptor.getMethodParameter()).thenReturn(methodParameter);
        assertFilter();
    }

    @Test
    public void doNotFilterAtAll() throws Exception
    {
        filter.filter(candidates, dependencyDescriptor);
        assertThat(candidates.containsKey(NAMED_FIELD), is(true));
        assertThat(candidates.containsKey(DISCARDED_KEY), is(true));
    }

    private void assertFilter()
    {
        Map<String, Object> filteredCandidates = filter.filter(candidates, dependencyDescriptor);
        assertThat(filteredCandidates.size(), is(1));
        assertThat(filteredCandidates.containsKey(NAMED_FIELD), is(true));
        assertThat(filteredCandidates.get(NAMED_FIELD), is(sameInstance(WANTED_VALUE)));
    }

    @Qualifier
    @Retention(RUNTIME)
    @Target({TYPE, ElementType.METHOD, FIELD, PARAMETER})
    public @interface TestAppleQualifier
    {

    }

    @TestAppleQualifier
    public static class QualifiedApple extends Apple
    {

    }

}
