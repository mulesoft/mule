/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.function.extension;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.util.Objects;

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

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    SimplePojo that = (SimplePojo) o;
    return Objects.equals(user, that.user) &&
        Objects.equals(pass, that.pass);
  }

  @Override
  public int hashCode() {
    return Objects.hash(user, pass);
  }
}
