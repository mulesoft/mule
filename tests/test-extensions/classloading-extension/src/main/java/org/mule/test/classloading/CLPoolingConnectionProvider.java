/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.classloading;

import static org.mule.test.classloading.api.ClassLoadingHelper.addClassLoader;
import org.mule.runtime.api.connection.PoolingConnectionProvider;
import org.mule.runtime.extension.api.annotation.Alias;

@Alias("pooling")
public class CLPoolingConnectionProvider extends CLNoneConnectionProvider implements PoolingConnectionProvider<String> {

  public static final String ON_BORROW = "OnBorrow";
  public static final String ON_RETURN = "OnReturn";

  @Override
  public void onBorrow(String connection) {
    addClassLoader(ON_BORROW + getKind());
  }

  @Override
  public void onReturn(String connection) {
    addClassLoader(ON_RETURN + getKind());
  }

  @Override
  public String getKind() {
    return "POOLING";
  }
}
