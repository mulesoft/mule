/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.streaming;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.internal.streaming.CursorUtils.unwrap;
import static org.mule.test.allure.AllureConstants.StreamingFeature.STREAMING;
import static org.mule.test.allure.AllureConstants.StreamingFeature.StreamingStory.STREAM_MANAGEMENT;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

@Feature(STREAMING)
@Story(STREAM_MANAGEMENT)
public class CursorUtilTestCase extends AbstractMuleTestCase {

  @Test
  @Issue("MULE-18573")
  public void unwrapNonDecoratedProvider() {
    CursorProvider provider = mock(CursorProvider.class);
    assertThat(unwrap(provider), is(sameInstance(provider)));
  }

  @Test
  @Issue("MULE-18573")
  public void unwrapOneLevelDecorator() {
    CursorProvider provider = mock(CursorProvider.class);
    CursorProviderDecorator decorator = mock(CursorProviderDecorator.class);
    when(decorator.getDelegate()).thenReturn(provider);

    assertThat(unwrap(decorator), is(sameInstance(provider)));
  }

  @Test
  @Issue("MULE-18573")
  public void unwrap42thLevelDecorator() {
    CursorProvider provider = mock(CursorProvider.class);
    CursorProviderDecorator decorator = mock(CursorProviderDecorator.class);
    when(decorator.getDelegate()).thenReturn(provider);

    for (int i = 0; i < 41; i++) {
      CursorProviderDecorator newDecorator = mock(CursorProviderDecorator.class);
      when(newDecorator.getDelegate()).thenReturn(decorator);
      decorator = newDecorator;
    }

    assertThat(unwrap(decorator), is(sameInstance(provider)));
  }
}
