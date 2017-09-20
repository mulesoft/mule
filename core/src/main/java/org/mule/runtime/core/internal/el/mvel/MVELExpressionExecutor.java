/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.el.mvel;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.internal.el.ExpressionExecutor;
import org.mule.runtime.core.api.expression.InvalidExpressionException;
import org.mule.mvel2.MVEL;
import org.mule.mvel2.ParserConfiguration;
import org.mule.mvel2.ParserContext;
import org.mule.mvel2.optimizers.OptimizerFactory;
import org.mule.mvel2.optimizers.dynamic.DynamicOptimizer;
import org.mule.mvel2.optimizers.impl.refl.ReflectiveAccessorOptimizer;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This MVEL executor uses MVEL {@link ReflectiveAccessorOptimizer} implementation rather than the default
 * {@link DynamicOptimizer} (which generates byte-code accessors using ASM) because we found that, at least with JDK7, the
 * {@link ReflectiveAccessorOptimizer} was fastest in typical Mule use cases.
 */
public class MVELExpressionExecutor implements ExpressionExecutor<MVELExpressionLanguageContext> {

  private static Logger log = LoggerFactory.getLogger(MVELExpressionExecutor.class);
  protected static final String DISABLE_MEL_EXPRESSION_CACHE =
      MuleProperties.SYSTEM_PROPERTY_PREFIX + "disableMelExpressionCache";

  protected static final int COMPILED_EXPRESSION_MAX_CACHE_SIZE = 1000;

  protected ParserConfiguration parserConfiguration;

  private LoadingCache<String, Serializable> compiledExpressionsCache;

  public MVELExpressionExecutor(final ParserConfiguration parserConfiguration) {
    this.parserConfiguration = parserConfiguration;

    MVEL.COMPILER_OPT_PROPERTY_ACCESS_DOESNT_FAIL = true;
    OptimizerFactory.setDefaultOptimizer(OptimizerFactory.SAFE_REFLECTIVE);

    compiledExpressionsCache =
        CacheBuilder.newBuilder().maximumSize(getCompiledExpressionMaxCacheSize()).build(new CacheLoader<String, Serializable>() {

          @Override
          public Serializable load(String key) throws Exception {
            return MVEL.compileExpression(key, new ParserContext(parserConfiguration));
          }
        });
  }

  private int getCompiledExpressionMaxCacheSize() {
    final String propertyValue = System.getProperty(DISABLE_MEL_EXPRESSION_CACHE);
    if (propertyValue != null) {
      return 0;
    } else {
      return COMPILED_EXPRESSION_MAX_CACHE_SIZE;
    }
  }

  @Override
  public Object execute(String expression, MVELExpressionLanguageContext context) {
    if (log.isTraceEnabled()) {
      log.trace("Executing MVEL expression '" + expression + "' with context: \n" + context.toString());
    }
    return MVEL.executeExpression(getCompiledExpression(expression), context);
  }

  @Override
  public void validate(String expression) throws InvalidExpressionException {
    getCompiledExpression(expression);
  }

  /**
   * Compile an expression. If such expression was compiled before then return the compilation output from a cache.
   * 
   * @param expression Expression to be compiled
   * @return A {@link Serializable} object representing the compiled expression
   */
  public Serializable getCompiledExpression(final String expression) {
    try {
      return compiledExpressionsCache.getUnchecked(expression);
    } catch (UncheckedExecutionException e) {
      // While exception is called UncheckedExecutionException and it generally wraps a RuntimeException
      // only the javadoc states that a non-runtime exception is also possible.
      if (e.getCause() instanceof RuntimeException) {
        throw (RuntimeException) e.getCause();
      } else {
        throw new MuleRuntimeException(e);
      }
    }
  }
}
