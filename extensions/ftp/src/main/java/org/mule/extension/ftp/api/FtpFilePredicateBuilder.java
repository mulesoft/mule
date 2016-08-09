/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ftp.api;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.module.extension.file.api.FilePredicateBuilder;

import java.time.LocalDateTime;
import java.util.function.Predicate;

/**
 * A specialization of {@link FilePredicateBuilder} used to do assertions on files stored on an FTP server. The file's properties
 * are to be represented on an instance of {@link FtpFileAttributes}
 * <p>
 * It adds capabilities to consider the file's unique timestamp.
 *
 * @since 4.0
 */
@Alias("matcher")
public class FtpFilePredicateBuilder extends FilePredicateBuilder<FtpFilePredicateBuilder, FtpFileAttributes> {

  /**
   * Files created before this date are rejected.
   */
  @Parameter
  @Optional
  private LocalDateTime timestampSince;

  /**
   * Files created after this date are rejected.
   */
  @Parameter
  @Optional
  private LocalDateTime timestampUntil;

  @Override
  protected Predicate<FtpFileAttributes> addConditions(Predicate<FtpFileAttributes> predicate) {
    if (timestampSince != null) {
      predicate = predicate.and(attributes -> FILE_TIME_SINCE.apply(timestampSince, attributes.getTimestamp()));
    }

    if (timestampUntil != null) {
      predicate = predicate.and(attributes -> FILE_TIME_UNTIL.apply(timestampUntil, attributes.getTimestamp()));
    }

    return predicate;
  }

  public FtpFilePredicateBuilder setTimestampSince(LocalDateTime timestampSince) {
    this.timestampSince = timestampSince;
    return this;
  }

  public FtpFilePredicateBuilder setTimestampUntil(LocalDateTime timestampUntil) {
    this.timestampUntil = timestampUntil;
    return this;
  }
}
