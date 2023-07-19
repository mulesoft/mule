/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.classloader;

import org.mule.module.artifact.classloader.JdbcResourceReleaser;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MySqlTestResourceReleaser extends JdbcResourceReleaser {

  public MySqlTestResourceReleaser() {}

  @Override
  public void release() {
    try {
      Method findMySqlDriverClassMethod = this.getClass().getSuperclass().getDeclaredMethod("findMySqlDriverClass");
      findMySqlDriverClassMethod.setAccessible(true);
      Class<?> foundClass = (Class<?>) findMySqlDriverClassMethod.invoke(this);

      MySqlDriverLookupTestCase.foundClassname = foundClass.getCanonicalName();
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      e.printStackTrace();
    }
  }



}
