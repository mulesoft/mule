/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser;

import static java.util.Optional.ofNullable;

import org.mule.runtime.api.meta.model.error.ErrorModel;
import org.mule.runtime.module.extension.internal.loader.parser.ErrorModelParser;

import java.util.Optional;

/**
 * {@link ErrorModelParser} implementation for errors defined through the Mule SDK.
 *
 * @since 4.5.0
 */
// TODO: Add test? Would it be so obvious?
public class MuleSdkErrorModelParser implements ErrorModelParser {

  private final String namespace;
  private final String type;
  private final ErrorModelParser parent;

  /**
   * Create a new instance since an {@link ErrorModel}.
   *
   * @param errorModel the {@link ErrorModel} with the error data.
   */
  public MuleSdkErrorModelParser(ErrorModel errorModel) {
    this(errorModel.getNamespace(), errorModel.getType(), errorModel.getParent().map(MuleSdkErrorModelParser::new).orElse(null));
  }

  /**
   * Create a new instance since the namespace, type, and parent.
   *
   * @param namespace the error namespace.
   * @param type      the error type.
   * @param parent    a reference to the parent error parser. If {@code null}, the method #getParent will return an empty instance
   *                  of {@link Optional}, and it will end up meaning MULE:ANY.
   */
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
    return ofNullable(parent);
  }
}
