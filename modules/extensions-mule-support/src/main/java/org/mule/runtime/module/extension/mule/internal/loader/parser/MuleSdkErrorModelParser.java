/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser;

import org.mule.runtime.api.meta.model.error.ErrorModel;
import org.mule.runtime.module.extension.internal.loader.parser.ErrorModelParser;

import java.util.Optional;

public class MuleSdkErrorModelParser implements ErrorModelParser {

  private final String namespace;
  private final String type;
  private final ErrorModelParser parent;

  public MuleSdkErrorModelParser(ErrorModel errorModel) {
    this(errorModel.getNamespace(), errorModel.getType(), errorModel.getParent().map(MuleSdkErrorModelParser::new).orElse(null));
  }

  public MuleSdkErrorModelParser(String namespace, String type) {
    this(namespace, type, null);
  }

  public MuleSdkErrorModelParser(String namespace, String type, ErrorModelParser parent) {
    this.namespace = namespace;
    this.type = type;
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
    return Optional.ofNullable(parent);
  }
}
