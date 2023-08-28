/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.loader.parser;

import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;

import static java.util.Locale.ROOT;
import static java.util.Optional.ofNullable;

import java.util.Optional;

/**
 * Base {@link ErrorModelParser} implementation.
 *
 * @since 4.5.0
 */
public class BaseErrorModelParser implements ErrorModelParser {

  private static final String MULE = CORE_PREFIX.toUpperCase(ROOT);

  private final String namespace;
  private final String type;
  private final boolean isMuleError;

  private ErrorModelParser parent;

  /**
   * Create a new instance since the namespace, type, and parent.
   *
   * @param namespace the error namespace.
   * @param type      the error type.
   */
  protected BaseErrorModelParser(String namespace, String type) {
    this.namespace = namespace;
    this.type = type;
    this.isMuleError = MULE.equals(namespace);
    this.parent = null;
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
  public boolean isMuleError() {
    return isMuleError;
  }

  @Override
  public Optional<ErrorModelParser> getParent() {
    return ofNullable(parent);
  }

  protected void setParent(ErrorModelParser parent) {
    this.parent = parent;
  }
}
