/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.typed.value.extension.extension;

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
