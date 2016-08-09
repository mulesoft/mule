/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.file.api;

import org.mule.runtime.core.message.OutputHandler;

import java.io.InputStream;

/**
 * Implementation of the visitor pattern meant to operate over the type of a file's content.
 * <p>
 * This interface includes default implementations of all the visit methods so that that implementors can implement only he
 * methods that result meaningful on each particular use case.
 *
 * @since 4.0
 */
public interface FileContentVisitor {

  /**
   * Invoked when the file has {@link String} content
   *
   * @param content the file's content
   * @throws Exception
   */
  default void visit(String content) throws Exception {}

  /**
   * Invoked when the file has one single {@code byte} as content
   *
   * @param content the file's content
   * @throws Exception
   */
  default void visit(byte content) throws Exception {}

  /**
   * Invoked when the file has one single {@code byte[]} as content
   *
   * @param content the file's content
   * @throws Exception
   */
  default void visit(byte[] content) throws Exception {}

  /**
   * Invoked when the file has an {@link OutputHandler} as content
   *
   * @param handler the file's content
   * @throws Exception
   */
  default void visit(OutputHandler handler) throws Exception {}

  /**
   * Invoked when the file has an {@link InputStream} as content
   *
   * @param content the file's content
   * @throws Exception
   */
  default void visit(InputStream content) throws Exception {}
}
