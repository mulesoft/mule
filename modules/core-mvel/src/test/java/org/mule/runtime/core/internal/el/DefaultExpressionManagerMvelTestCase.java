/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el;

import static org.mule.runtime.api.metadata.DataType.BYTE_ARRAY;
import static org.mule.runtime.core.api.config.MuleProperties.COMPATIBILITY_PLUGIN_INSTALLED;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_EXPRESSION_LANGUAGE;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.test.allure.AllureConstants.ExpressionLanguageFeature.EXPRESSION_LANGUAGE;
import static org.mule.test.allure.AllureConstants.ExpressionLanguageFeature.ExpressionLanguageStory.SUPPORT_MVEL_DW;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.internal.el.mvel.MVELExpressionLanguage;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(EXPRESSION_LANGUAGE)
@Story(SUPPORT_MVEL_DW)
public class DefaultExpressionManagerMvelTestCase extends AbstractMuleContextTestCase {

  @Rule
  public MockitoRule mockitorule = MockitoJUnit.rule();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private StreamingManager streamingManager;

  private ExtendedExpressionManager expressionManager;

  @Before
  public void configureExpressionManager() throws MuleException {
    expressionManager = muleContext.getExpressionManager();
    ((DefaultExpressionManager) expressionManager).setMelDefault(false);
  }

  @Override
  protected boolean mockExprExecutorService() {
    return true;
  }

  @Test
  public void managedCursor() throws Exception {
    final ExtendedExpressionLanguageAdaptor expressionLanguage =
        mock(ExtendedExpressionLanguageAdaptor.class, RETURNS_DEEP_STUBS);
    final CursorProvider cursorProvider = mock(CursorProvider.class);

    Registry registry = mock(Registry.class);
    when(registry.lookupByName(OBJECT_EXPRESSION_LANGUAGE))
        .thenReturn(of(mock(MVELExpressionLanguage.class, RETURNS_DEEP_STUBS)));
    when(registry.lookupByName(COMPATIBILITY_PLUGIN_INSTALLED)).thenReturn(empty());

    TypedValue value = new TypedValue(cursorProvider, BYTE_ARRAY);
    when(expressionLanguage.evaluate(anyString(), any(), any(), any(), any())).thenReturn(value);

    ((DefaultExpressionManager) expressionManager)
        .setExpressionLanguage(expressionLanguage);
    ((DefaultExpressionManager) expressionManager).setStreamingManager(streamingManager);
    initialiseIfNeeded(expressionManager, false, muleContext);
    final CoreEvent event = testEvent();

    when(streamingManager.manage(cursorProvider, event.getContext())).thenReturn(cursorProvider);

    expressionManager.evaluate("someExpression", event);
    verify(streamingManager).manage(cursorProvider, event.getContext());
  }

}
