/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.runtime.resolver;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mule.module.extensions.internal.util.ExtensionsTestUtils.getResolver;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Lifecycle;
import org.mule.module.extensions.internal.util.ExtensionsTestUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class CollectionValueResolverTestCase extends AbstractMuleTestCase
{

    private Class<? extends Collection> collectionType;

    @Parameterized.Parameters
    public static Collection<Object[]> data()
    {
        return Arrays.asList(new Object[][] {{ArrayList.class}, {HashSet.class}});
    }

    private CollectionValueResolver resolver;
    private List<ValueResolver> childResolvers;
    private List<Integer> expectedValues;
    private MuleContext muleContext;
    private MuleEvent event;

    public CollectionValueResolverTestCase(Class<? extends Collection> collectionType)
    {
        this.collectionType = collectionType;
    }

    @Before
    public void before() throws Exception
    {
        muleContext = mock(MuleContext.class);
        event = mock(MuleEvent.class);

        collectionType = ArrayList.class;
        childResolvers = new ArrayList();
        expectedValues = new ArrayList<>();

        for (int i = 0; i < getChildResolversCount(); i++)
        {
            ValueResolver childResolver = getResolver(i, event, false, MuleContextAware.class, Lifecycle.class);
            childResolvers.add(childResolver);
            expectedValues.add(i);
        }

        resolver = createCollectionResolver(childResolvers);
    }

    @Test
    public void resolve() throws Exception
    {
        Collection<Object> resolved = (Collection<Object>) resolver.resolve(event);

        assertThat(resolved, notNullValue());
        assertThat(resolved.size(), equalTo(getChildResolversCount()));
        assertThat(resolved, hasItems(expectedValues.toArray()));
    }

    @Test
    public void resolversAreCopied() throws Exception
    {
        int initialResolversCount = childResolvers.size();
        childResolvers.add(ExtensionsTestUtils.getResolver(-1, event, false));

        Collection<Object> resolved = (Collection<Object>) resolver.resolve(event);
        assertThat(resolved.size(), equalTo(initialResolversCount));
    }

    @Test
    public void emptyList() throws Exception
    {
        childResolvers.clear();
        resolver = createCollectionResolver(childResolvers);

        Collection<Object> resolved = (Collection<Object>) resolver.resolve(mock(MuleEvent.class));
        assertThat(resolved, notNullValue());
        assertThat(resolved.size(), equalTo(0));
    }

    @Test
    public void isNotDynamic()
    {
        assertThat(resolver.isDynamic(), is(false));
    }

    @Test
    public void isDynamic() throws Exception
    {
        childResolvers = new ArrayList();
        childResolvers.add(getResolver(null, event, false));
        childResolvers.add(getResolver(null, event, true));

        resolver = createCollectionResolver(childResolvers);
        assertThat(resolver.isDynamic(), is(true));
    }

    @Test
    public void collectionOfExpectedType() throws Exception
    {
        Collection<Object> resolved = (Collection<Object>) resolver.resolve(mock(MuleEvent.class));
        assertThat(resolved, instanceOf(collectionType));
    }

    @Test
    public void resolvedCollectionIsMutalbe() throws Exception
    {
        Collection<Object> resolved = (Collection<Object>) resolver.resolve(mock(MuleEvent.class));
        int originalSize = resolved.size();
        resolved.add(-1);

        assertThat(resolved.size(), equalTo(originalSize + 1));
    }

    @Test
    public void initialise() throws Exception
    {
        resolver.setMuleContext(muleContext);
        resolver.initialise();
        ExtensionsTestUtils.verifyAllInitialised(childResolvers, muleContext);
    }

    @Test
    public void start() throws Exception
    {
        resolver.start();
        ExtensionsTestUtils.verifyAllStarted(childResolvers);
    }

    @Test
    public void stop() throws Exception
    {
        resolver.stop();
        ExtensionsTestUtils.verifyAllStopped(childResolvers);
    }

    @Test
    public void dispose() throws Exception
    {
        resolver.dispose();
        ExtensionsTestUtils.verifyAllDisposed(childResolvers);
    }

    protected int getChildResolversCount()
    {
        return 10;
    }

    private CollectionValueResolver createCollectionResolver(List<ValueResolver> childResolvers)
    {
        return new CollectionValueResolver(collectionType, childResolvers);
    }

    protected void doAssertOf(Class<? extends Collection> collectionType, Class<? extends ValueResolver> expectedResolverType)
    {
        ValueResolver resolver = new CollectionValueResolver(mock(collectionType).getClass(), new ArrayList<ValueResolver>());
        assertThat(resolver.getClass() == expectedResolverType, is(true));
    }
}
