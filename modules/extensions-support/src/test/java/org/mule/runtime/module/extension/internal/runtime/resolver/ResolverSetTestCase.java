/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.getParameter;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.test.module.extension.internal.util.ExtensionsTestUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ResolverSetTestCase extends AbstractMuleTestCase {

  private static final String NAME = "MG";
  private static final int AGE = 31;

  private ResolverSet set;
  private Map<ParameterModel, ValueResolver> mapping;

  @Mock
  private CoreEvent event;

  @Mock
  private ValueResolvingContext resolvingContext;

  @Mock
  private MuleContext muleContext;

  @Before
  public void before() throws Exception {
    mapping = new LinkedHashMap<>();
    mapping.put(getParameter("myName", String.class), getResolver(NAME));
    mapping.put(getParameter("age", Integer.class), getResolver(AGE));

    when(resolvingContext.getEvent()).thenReturn(event);
    when(resolvingContext.getConfig()).thenReturn(Optional.empty());

    set = buildSet(mapping);
  }

  @Test
  public void resolve() throws Exception {
    ResolverSetResult result = set.resolve(resolvingContext);
    assertResult(result, mapping);
  }

  @Test(expected = IllegalArgumentException.class)
  public void addNullParameter() throws Exception {
    set.add(null, getResolver(null));
  }

  @Test(expected = IllegalArgumentException.class)
  public void addNullresolver() throws Exception {
    set.add("blah", null);
  }

  @Test(expected = IllegalStateException.class)
  public void addRepeatedParameter() throws Exception {
    final String parameterName = "name";
    ValueResolver<String> resolver = getResolver(NAME);

    set.add(parameterName, resolver);
    set.add(parameterName, resolver);
  }

  @Test
  public void isNotDynamic() {
    assertThat(set.isDynamic(), is(false));
  }

  @Test
  public void isDynamic() throws Exception {
    ValueResolver resolver = getResolver(null);
    when(resolver.isDynamic()).thenReturn(true);

    set.add("whatever", resolver);
    assertThat(set.isDynamic(), is(true));
  }

  private void assertResult(ResolverSetResult result, Map<ParameterModel, ValueResolver> mapping) throws Exception {
    assertThat(result, is(notNullValue()));
    for (Map.Entry<ParameterModel, ValueResolver> entry : mapping.entrySet()) {
      Object value = result.get(entry.getKey().getName());
      assertThat(value, is(entry.getValue().resolve(resolvingContext)));
    }
  }

  private ResolverSet buildSet(Map<ParameterModel, ValueResolver> mapping) {
    ResolverSet set = new ResolverSet(muleContext);
    mapping.forEach((key, value) -> set.add(key.getName(), value));

    return set;
  }

  private ValueResolver getResolver(Object value) throws Exception {
    return ExtensionsTestUtils.getResolver(value, resolvingContext, false, MuleContextAware.class, Lifecycle.class);
  }
}
