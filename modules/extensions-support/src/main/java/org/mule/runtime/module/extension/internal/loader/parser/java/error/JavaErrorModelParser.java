/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java.error;

import static java.util.Optional.ofNullable;

import org.mule.runtime.module.extension.internal.loader.parser.ErrorModelParser;

import java.util.Optional;

public class JavaErrorModelParser implements ErrorModelParser {

  private final String type;
  private final String namespace;
  private final ErrorModelParser parent;

  public JavaErrorModelParser(String type, String namespace, ErrorModelParser parent) {
    this.type = type;
    this.namespace = namespace;
    this.parent = parent;
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  public String getNamespace() {
    return namespace;
  }

  @Override
  public Optional<ErrorModelParser> getParent() {
    return ofNullable(parent);
  }
}
