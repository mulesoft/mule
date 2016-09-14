/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api.predicate;

import org.mule.extension.email.api.EmailAttributes;
import org.mule.extension.email.api.ImapEmailAttributes;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;

import java.util.function.Predicate;

/**
 *
 * @since 4.0
 */
@Alias("imap-matcher")
public class ImapEmailPredicateBuilder extends DefaultEmailPredicateBuilder {

  /**
   * Indicates if should retrieve 'seen' or 'not seen' emails
   */
  @Parameter
  @Optional
  private Boolean seen;

  /**
   * Indicates if should retrieve 'answered' or 'not answered' emails
   */
  @Parameter
  @Optional
  private Boolean answered;

  /**
   * Indicates if should retrieve 'marked as deleted' or 'not marked as deleted' emails
   */
  @Parameter
  @Optional
  private Boolean deleted;

  /**
   * "Indicates if should retrieve 'recent' or 'not recent' emails
   */
  @Parameter
  @Optional
  private Boolean recent;


  @Override
  protected Predicate<? extends EmailAttributes> getBasePredicate() {

    Predicate<ImapEmailAttributes> predicate = imapEmailAttributes -> true;

    if (recent != null) {
      predicate = predicate.and(attributes -> recent == attributes.getFlags().isRecent());
    }

    if (deleted != null) {
      predicate = predicate.and(attributes -> deleted == attributes.getFlags().isDeleted());
    }

    if (answered != null) {
      predicate = predicate.and(attributes -> answered == attributes.getFlags().isAnswered());
    }

    if (seen != null) {
      predicate = predicate.and(attributes -> seen == attributes.getFlags().isSeen());
    }

    return predicate;
  }

  public ImapEmailPredicateBuilder setAnswered(Boolean answered) {
    this.answered = answered;
    return this;
  }

  public ImapEmailPredicateBuilder setDeleted(Boolean deleted) {
    this.deleted = deleted;
    return this;
  }

  public ImapEmailPredicateBuilder setRecent(Boolean recent) {
    this.recent = recent;
    return this;
  }

  public ImapEmailPredicateBuilder setSeen(Boolean seen) {
    this.seen = seen;
    return this;
  }
}
