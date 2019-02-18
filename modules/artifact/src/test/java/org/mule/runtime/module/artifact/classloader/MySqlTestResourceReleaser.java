/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.classloader;

import org.mule.module.artifact.classloader.DefaultResourceReleaser;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MySqlTestResourceReleaser extends DefaultResourceReleaser {

  public MySqlTestResourceReleaser() {}

  @Override
  public void release() {
    try {
      Method findMySqlDriverClassMethod = this.getClass().getSuperclass().getDeclaredMethod("findMySqlDriverClass");
      findMySqlDriverClassMethod.setAccessible(true);
      Class<?> foundClass = (Class<?>) findMySqlDriverClassMethod.invoke(this);

      MySqlResourceReleaserTestCase.foundClassname = foundClass.getCanonicalName();
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      e.printStackTrace();
    }
  }



}
