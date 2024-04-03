/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
/**
 * Mule Runtime Errors.
 *
 * @provides org.mule.runtime.ast.api.error.ErrorTypeRepositoryProvider
 *
 * @moduleGraph
 * @since 4.7
 */
module org.mule.runtime.errors {

  requires org.mule.runtime.api;
  requires org.mule.runtime.artifact.ast;

  exports org.mule.runtime.core.api.error;
  exports org.mule.runtime.config.internal.error to
      org.mule.runtime.core,
      org.mule.runtime.extensions.mule.support,
      org.mule.runtime.spring.config,
      org.mule.runtime.artifact.ast.serialization.test;

  provides org.mule.runtime.ast.api.error.ErrorTypeRepositoryProvider with
      org.mule.runtime.config.internal.error.CoreErrorTypeRepositoryProvider;

}