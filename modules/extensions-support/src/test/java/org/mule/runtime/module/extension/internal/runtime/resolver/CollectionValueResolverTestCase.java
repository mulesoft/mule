/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.getResolver;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.test.module.extension.internal.util.ExtensionsTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class CollectionValueResolverTestCase extends AbstractMuleTestCase {

  private Class<? extends Collection> collectionType;
  private ValueResolvingContext resolvingContext;
  private CollectionValueResolver resolver;
  private List<ValueResolver> childResolvers;
  private List<Integer> expectedValues;
  private MuleContext muleContext;
  private CoreEvent event;

  @Parameterized.Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {{ArrayList.class}, {HashSet.class}});
  }

  public CollectionValueResolverTestCase(Class<? extends Collection> collectionType) {
    this.collectionType = collectionType;
  }

  @Before
  public void before() throws Exception {
    muleContext = mock(MuleContext.class);
    event = mock(CoreEvent.class);
    resolvingContext = mock(ValueResolvingContext.class);

    when(resolvingContext.getEvent()).thenReturn(event);
    when(resolvingContext.getConfig()).thenReturn(Optional.empty());

    collectionType = ArrayList.class;
    childResolvers = new ArrayList();
    expectedValues = new ArrayList<>();

    for (int i = 0; i < getChildResolversCount(); i++) {
      ValueResolver childResolver = getResolver(i, resolvingContext, false, MuleContextAware.class, Lifecycle.class);
      childResolvers.add(childResolver);
      expectedValues.add(i);
    }

    resolver = createCollectionResolver(childResolvers);
  }

  @Test
  public void resolve() throws Exception {
    Collection<Object> resolved = (Collection<Object>) resolver.resolve(resolvingContext);

    assertThat(resolved, notNullValue());
    assertThat(resolved.size(), equalTo(getChildResolversCount()));
    assertThat(resolved, hasItems(expectedValues.toArray()));
  }

  @Test
  public void resolversAreCopied() throws Exception {
    int initialResolversCount = childResolvers.size();
    childResolvers.add(ExtensionsTestUtils.getResolver(-1, resolvingContext, false));

    Collection<Object> resolved = (Collection<Object>) resolver.resolve(resolvingContext);
    assertThat(resolved.size(), equalTo(initialResolversCount));
  }

  @Test
  public void emptyList() throws Exception {
    childResolvers.clear();
    resolver = createCollectionResolver(childResolvers);

    Collection<Object> resolved = (Collection<Object>) resolver.resolve(resolvingContext);
    assertThat(resolved, notNullValue());
    assertThat(resolved.size(), equalTo(0));
  }

  @Test
  public void isNotDynamic() {
    assertThat(resolver.isDynamic(), is(false));
  }

  @Test
  public void isDynamic() throws Exception {
    childResolvers = new ArrayList();
    childResolvers.add(getResolver(null, resolvingContext, false));
    childResolvers.add(getResolver(null, resolvingContext, true));

    resolver = createCollectionResolver(childResolvers);
    assertThat(resolver.isDynamic(), is(true));
  }

  @Test
  public void collectionOfExpectedType() throws Exception {
    Collection<Object> resolved = (Collection<Object>) resolver.resolve(resolvingContext);
    assertThat(resolved, instanceOf(collectionType));
  }

  @Test
  public void resolvedCollectionIsMutalbe() throws Exception {
    Collection<Object> resolved = (Collection<Object>) resolver.resolve(resolvingContext);
    int originalSize = resolved.size();
    resolved.add(-1);

    assertThat(resolved.size(), equalTo(originalSize + 1));
  }

  protected int getChildResolversCount() {
    return 10;
  }

  private CollectionValueResolver createCollectionResolver(List<ValueResolver> childResolvers) {
    return new CollectionValueResolver(collectionType, childResolvers);
  }

  protected void doAssertOf(Class<? extends Collection> collectionType, Class<? extends ValueResolver> expectedResolverType) {
    ValueResolver resolver = new CollectionValueResolver(mock(collectionType).getClass(), new ArrayList<ValueResolver>());
    assertThat(resolver.getClass() == expectedResolverType, is(true));
  }
}
