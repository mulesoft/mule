/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.junit4;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.MuleContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.Annotatable;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

/**
 * Invokes the tests annotated with {@link StopStartTest} twice, stopping and starting the muleContext
 * between both invocations.
 *
 * The restart process consists in:
 * 1 - stop the {@link MuleContext} if it's started (some tests will stop it themselves)
 * 2 - initialise the {@link MuleContext} if it isn't initialized (some tests dispose it)
 * 3 - start the the {@link MuleContext}
 *
 * Throws an {@link InitializationError} if the test class hasn't a {@link MuleContext} field.
 */
public class StopStartTestRunner extends BlockJUnit4ClassRunner {

  private final Field muleContextField;

  public StopStartTestRunner(Class<?> type) throws InitializationError {
    super(type);
    muleContextField = getMuleContextFieldFromClass(type);
  }

  private Field getMuleContextFieldFromClass(Class<?> type) throws InitializationError {
    if (type == null) {
      throw new InitializationError("Test class hasn't a MuleContext field");
    }

    for (Field field : type.getDeclaredFields()) {
      if (MuleContext.class.isAssignableFrom(field.getType())) {
        return field;
      }
    }

    return getMuleContextFieldFromClass(type.getSuperclass());
  }

  @Override
  protected Statement methodInvoker(FrameworkMethod method, Object test) {
    Statement singleStatement = super.methodInvoker(method, test);

    if (hasAnnotation(method) || hasAnnotation(method.getDeclaringClass())) {
      return new Statement() {

        @Override
        public void evaluate() throws Throwable {
          MuleContext muleContext = (MuleContext) muleContextField.get(test);
          singleStatement.evaluate();
          restart(muleContext);
          singleStatement.evaluate();
        }
      };
    } else {
      return singleStatement;
    }
  }

  private boolean hasAnnotation(FrameworkMethod method) {
    return method.getAnnotation(StopStartTest.class) != null;
  }

  private boolean hasAnnotation(Class clazz) {
    if (clazz == null) {
      return false;
    }

    Annotation annotation = clazz.getAnnotation(StopStartTest.class);
    if (annotation != null) {
      return true;
    }

    return hasAnnotation(clazz.getSuperclass());
  }

  private static void restart(MuleContext muleContext) throws MuleException {
    if (muleContext.isStarted()) {
      muleContext.stop();
    }

    if (!muleContext.isInitialised()) {
      muleContext.initialise();
    }

    muleContext.start();
  }

}
