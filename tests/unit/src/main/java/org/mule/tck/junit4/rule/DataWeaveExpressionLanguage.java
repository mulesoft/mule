/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.junit4.rule;

import static org.mule.runtime.api.el.BindingContextUtils.NULL_BINDING_CONTEXT;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.tck.config.WeaveExpressionLanguageFactoryServiceProvider.provideDefaultExpressionLanguageFactoryService;

import static java.nio.charset.StandardCharsets.UTF_8;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.el.DefaultExpressionLanguageFactoryService;
import org.mule.runtime.api.el.ExpressionLanguage;
import org.mule.runtime.api.el.ExpressionLanguageConfiguration;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.internal.el.DefaultExpressionManager;
import org.mule.runtime.core.internal.el.dataweave.DataWeaveExpressionLanguageAdaptor;
import org.mule.runtime.core.internal.transformer.TransformersRegistry;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;

public class DataWeaveExpressionLanguage implements TestRule {

  // reuse the same DW instance across as many tests as possible to avoid reinitializing it every time
  private static DataWeaveExpressionLanguage INSTANCE = new DataWeaveExpressionLanguage();

  public static DataWeaveExpressionLanguage dataWeaveRule() {
    return INSTANCE;
  }

  private DefaultExpressionLanguageFactoryService cachedExprLanguageFactory;
  private int cachedExprLanguageFactoryCounter = 0;

  private DefaultExpressionManager expressionManager;

  public DefaultExpressionLanguageFactoryService getExprLanguageFactory() {
    initCachedExprLanguageFactory();
    return cachedExprLanguageFactory;
  }

  public ExtendedExpressionManager getExpressionManager() {
    return expressionManager;
  }

  @Override
  public Statement apply(Statement base, Description description) {

    return new Statement() {

      @Override
      public void evaluate() throws Throwable {
        final var muleConfiguration = mock(MuleConfiguration.class);
        when(muleConfiguration.isValidateExpressions()).thenReturn(true);

        final var transformer = mock(Transformer.class);
        when(transformer.transform(any(TypedValue.class))).thenAnswer(inv -> {
          final var value = inv.getArgument(0, TypedValue.class).getValue();
          if (value instanceof InputStream isValue) {
            return IOUtils.toString(isValue, UTF_8);
          }
          return value;
        });
        final var transformerRegistry = mock(TransformersRegistry.class);
        when(transformerRegistry.lookupTransformer(any(), any())).thenReturn(transformer);

        expressionManager = new DefaultExpressionManager();
        expressionManager.setMuleConfiguration(muleConfiguration);
        expressionManager.setTransformersRegistry(transformerRegistry);
        expressionManager.setStreamingManager(mock(StreamingManager.class));
        expressionManager.setExpressionLanguage(new DataWeaveExpressionLanguageAdaptor(null, // ?
                                                                                       null,
                                                                                       muleConfiguration,
                                                                                       () -> UTF_8,
                                                                                       getExprLanguageFactory(),
                                                                                       null));

        initialiseIfNeeded(expressionManager);

        List<Throwable> errors = new ArrayList<>();
        try {
          base.evaluate();
        } catch (Throwable t) {
          errors.add(t);
        }
        MultipleFailureException.assertEmpty(errors);
      }
    };
  }

  protected void initCachedExprLanguageFactory() {
    // Avoid doing the DW warm-up for every test, reusing the ExpressionLanguage implementation
    // Still have to recreate ever once in a while so global bindings added for each test are accumulated.
    if (cachedExprLanguageFactory == null || cachedExprLanguageFactoryCounter > 20) {
      cachedExprLanguageFactoryCounter = 0;
      final DefaultExpressionLanguageFactoryService exprExecutor = provideDefaultExpressionLanguageFactoryService();
      ExpressionLanguage exprLanguage = exprExecutor.create();
      // Force initialization of internal DataWeave stuff
      // This way we avoid doing some heavy initialization on the test itself,
      // which may cause trouble when evaluation expressions in places with small timeouts
      exprLanguage.evaluate("{dataWeave: 'is'} ++ {mule: 'default EL'}", NULL_BINDING_CONTEXT);

      cachedExprLanguageFactory = new DefaultExpressionLanguageFactoryService() {

        @Override
        public ExpressionLanguage create() {
          return exprLanguage;
        }

        @Override
        public ExpressionLanguage create(ExpressionLanguageConfiguration configuration) {
          return exprLanguage;
        }

        @Override
        public String getName() {
          return exprExecutor.getName();
        }
      };
    } else {
      cachedExprLanguageFactoryCounter++;
    }
  }

}
