/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser;

import static java.util.Optional.ofNullable;

import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.meta.model.error.ErrorModel;
import org.mule.runtime.module.extension.internal.loader.parser.BaseErrorModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.ErrorModelParser;

import java.util.Objects;
import java.util.Optional;

/**
 * {@link ErrorModelParser} implementation for errors defined through the Mule SDK.
 *
 * @since 4.5.0
 */
public class MuleSdkErrorModelParser extends BaseErrorModelParser {

  private boolean suppressed;

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
    super(namespace, type);
    setParent(parent);
  }

  /**
   * Create a new instance since an {@link ErrorType}.
   *
   * @param errorType the {@link ErrorType} with the error data.
   */
  public MuleSdkErrorModelParser(ErrorType errorType) {
    this(errorType.getNamespace(), errorType.getIdentifier(),
         ofNullable(errorType.getParentErrorType()).map(MuleSdkErrorModelParser::new).orElse(null));
  }

  public void setSuppressed() {
    this.suppressed = true;
  }

  @Override
  public boolean isSuppressed() {
    return suppressed;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getNamespace(), getType(), getParent());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MuleSdkErrorModelParser that = (MuleSdkErrorModelParser) o;
    return Objects.equals(this.getNamespace(), that.getNamespace())
        && Objects.equals(this.getType(), that.getType())
        && Objects.equals(this.getParent(), that.getParent());
  }
}
