/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.streaming;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.mule.runtime.core.internal.streaming.CursorUtils.unwrap;
import static org.mule.test.allure.AllureConstants.StreamingFeature.STREAMING;
import static org.mule.test.allure.AllureConstants.StreamingFeature.StreamingStory.STREAM_MANAGEMENT;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.api.streaming.object.CursorIteratorProvider;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

@Feature(STREAMING)
@Story(STREAM_MANAGEMENT)
public class IdentifiableCursorProviderDecoratorTestCase extends AbstractMuleTestCase {

  private static List<Integer> COLLECTED_IDS;
  private static AtomicInteger FAKE_ID_GENERATOR = new AtomicInteger(2000);

  @BeforeClass
  public static void beforeClass() {
    COLLECTED_IDS = new LinkedList<>();
  }

  @AfterClass
  public static void afterClass() {
    try {
      Set<Integer> uniqueIds = new HashSet<>(COLLECTED_IDS);
      assertThat(uniqueIds, hasSize(COLLECTED_IDS.size()));
    } finally {
      COLLECTED_IDS = null;
    }
  }

  @Test
  @Issue("MULE-18573")
  public void decorateSimpleCursorStreamProvider() {
    CursorStreamProvider provider = mock(CursorStreamProvider.class);
    IdentifiableCursorProviderDecorator decorator = IdentifiableCursorProviderDecorator.of(provider);
    assertThat(decorator.getDelegate(), is(sameInstance(provider)));
    assertThat(decorator, is(instanceOf(CursorStreamProvider.class)));
    getId(decorator);
  }

  @Test
  @Issue("MULE-18573")
  public void decorateSimpleCursorStreamProviderTwice() {
    CursorStreamProvider provider = mock(CursorStreamProvider.class);
    IdentifiableCursorProviderDecorator decorator = IdentifiableCursorProviderDecorator.of(provider);
    assertThat(decorator.getDelegate(), is(sameInstance(provider)));
    assertThat(decorator, is(instanceOf(CursorStreamProvider.class)));

    IdentifiableCursorProviderDecorator decoratorTwice = IdentifiableCursorProviderDecorator.of(decorator);
    assertThat(decoratorTwice.getDelegate(), is(sameInstance(provider)));
    assertThat(decoratorTwice, is(instanceOf(CursorStreamProvider.class)));
    assertThat(decoratorTwice, is(sameInstance(decorator)));

    getId(decorator);
  }

  @Test
  @Issue("MULE-18573")
  public void decorateSimpleCursorIteratorProvider() {
    CursorIteratorProvider provider = mock(CursorIteratorProvider.class);
    IdentifiableCursorProviderDecorator decorator = IdentifiableCursorProviderDecorator.of(provider);
    assertThat(decorator.getDelegate(), is(sameInstance(provider)));
    assertThat(decorator, is(instanceOf(CursorIteratorProvider.class)));
    getId(decorator);
  }

  @Test
  @Issue("MULE-18573")
  public void decorateSimpleCursorIteratorProviderTwice() {
    CursorIteratorProvider provider = mock(CursorIteratorProvider.class);
    IdentifiableCursorProviderDecorator decorator = IdentifiableCursorProviderDecorator.of(provider);
    assertThat(decorator.getDelegate(), is(sameInstance(provider)));
    assertThat(decorator, is(instanceOf(CursorIteratorProvider.class)));

    IdentifiableCursorProviderDecorator decoratorTwice = IdentifiableCursorProviderDecorator.of(decorator);
    assertThat(decoratorTwice.getDelegate(), is(sameInstance(provider)));
    assertThat(decoratorTwice, is(instanceOf(CursorIteratorProvider.class)));
    assertThat(decoratorTwice, is(sameInstance(decorator)));
    getId(decorator);
  }

  @Test
  @Issue("MULE-18573")
  public void decorateIdentifiableCursorStreamProvider() {
    IdentifiableCursorProvider provider =
        mock(IdentifiableCursorProvider.class, withSettings().extraInterfaces(CursorStreamProvider.class));
    final int id = FAKE_ID_GENERATOR.incrementAndGet();
    when(provider.getId()).thenReturn(id);

    IdentifiableCursorProviderDecorator decorator = IdentifiableCursorProviderDecorator.of(provider);
    assertThat(decorator.getDelegate(), is(sameInstance(provider)));
    assertThat(decorator, is(instanceOf(CursorStreamProvider.class)));
    assertThat(getId(provider), is(id));
  }

  @Test
  @Issue("MULE-18573")
  public void decorateIdentifiableCursorIteratorProvider() {
    IdentifiableCursorProvider provider =
        mock(IdentifiableCursorProvider.class, withSettings().extraInterfaces(CursorIteratorProvider.class));
    final int id = FAKE_ID_GENERATOR.incrementAndGet();
    when(provider.getId()).thenReturn(id);

    IdentifiableCursorProviderDecorator decorator = IdentifiableCursorProviderDecorator.of(provider);
    assertThat(decorator.getDelegate(), is(sameInstance(provider)));
    assertThat(decorator, is(instanceOf(CursorIteratorProvider.class)));
    assertThat(getId(decorator), is(id));
  }

  @Test
  @Issue("MULE-18573")
  public void doubleDecorateCursorStreamProvider() {
    CursorStreamProvider provider = mock(CursorStreamProvider.class);
    CursorProviderDecorator firstDecorator =
        mock(CursorProviderDecorator.class, withSettings().extraInterfaces(CursorStreamProvider.class));
    when(firstDecorator.getDelegate()).thenReturn(provider);

    IdentifiableCursorProviderDecorator decorator = IdentifiableCursorProviderDecorator.of(firstDecorator);
    assertThat(decorator.getDelegate(), is(sameInstance(firstDecorator)));
    assertThat(decorator, is(instanceOf(CursorStreamProvider.class)));
    getId(decorator);
  }

  @Test
  @Issue("MULE-18573")
  public void doubleDecorateIdentifiableCursorStreamProvider() {
    CursorStreamProvider provider = mock(CursorStreamProvider.class);
    CursorProviderDecorator firstDecorator =
        mock(CursorProviderDecorator.class,
             withSettings().extraInterfaces(CursorStreamProvider.class, IdentifiableCursorProvider.class));
    final int id = FAKE_ID_GENERATOR.incrementAndGet();
    when(firstDecorator.getDelegate()).thenReturn(provider);
    when(((IdentifiableCursorProvider) firstDecorator).getId()).thenReturn(id);

    IdentifiableCursorProviderDecorator decorator = IdentifiableCursorProviderDecorator.of(firstDecorator);
    assertThat(unwrap(decorator), is(sameInstance(provider)));
    assertThat(decorator, is(instanceOf(CursorStreamProvider.class)));
    assertThat(getId(decorator), is(id));
  }

  @Test
  @Issue("MULE-18573")
  public void doubleDecorateCursorIteratorProvider() {
    CursorIteratorProvider provider = mock(CursorIteratorProvider.class);
    CursorProviderDecorator firstDecorator =
        mock(CursorProviderDecorator.class, withSettings().extraInterfaces(CursorIteratorProvider.class));
    when(firstDecorator.getDelegate()).thenReturn(provider);

    IdentifiableCursorProviderDecorator decorator = IdentifiableCursorProviderDecorator.of(firstDecorator);
    assertThat(decorator.getDelegate(), is(sameInstance(firstDecorator)));
    assertThat(decorator, is(instanceOf(CursorIteratorProvider.class)));
    getId(decorator);
  }

  @Test
  @Issue("MULE-18573")
  public void doubleDecorateIdentifiableCursorIteratorProvider() {
    CursorIteratorProvider provider = mock(CursorIteratorProvider.class);
    CursorProviderDecorator firstDecorator =
        mock(CursorProviderDecorator.class,
             withSettings().extraInterfaces(CursorIteratorProvider.class, IdentifiableCursorProvider.class));

    final int id = FAKE_ID_GENERATOR.incrementAndGet();
    when(firstDecorator.getDelegate()).thenReturn(provider);
    when(((IdentifiableCursorProvider) firstDecorator).getId()).thenReturn(id);

    IdentifiableCursorProviderDecorator decorator = IdentifiableCursorProviderDecorator.of(firstDecorator);
    assertThat(unwrap(decorator), is(sameInstance(provider)));
    assertThat(decorator, is(instanceOf(CursorIteratorProvider.class)));
    assertThat(getId(decorator), is(id));
  }

  private int getId(IdentifiableCursorProvider provider) {
    int id = provider.getId();
    COLLECTED_IDS.add(id);

    return id;
  }
}

