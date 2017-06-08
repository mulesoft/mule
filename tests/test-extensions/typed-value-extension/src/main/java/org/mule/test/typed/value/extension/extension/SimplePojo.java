/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.typed.value.extension.extension;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

public class SimplePojo {

  @Parameter
  @Optional
  TypedValue<String> user;

  @Parameter
  @Optional
  String pass;

  public String getUser() {
    return user != null ? user.getValue() : "";
  }

  public void setUser(String user) {
    this.user = new TypedValue<>(user, DataType.STRING);
  }

  public String getPass() {
    return pass;
  }

  public void setPass(String pass) {
    this.pass = pass;
  }
}
