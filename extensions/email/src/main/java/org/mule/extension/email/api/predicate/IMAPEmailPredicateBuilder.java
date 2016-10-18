/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api.predicate;

import org.mule.extension.email.api.EmailFlags;
import org.mule.extension.email.api.attributes.BaseEmailAttributes;
import org.mule.extension.email.api.attributes.IMAPEmailAttributes;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.dsl.xml.XmlHints;
import org.mule.runtime.extension.api.annotation.param.Optional;

import java.util.function.Predicate;

/**
 * Builds a {@link Predicate} which verifies that a {@link IMAPEmailAttributes} instance is compliant with a number of criteria.
 * This builder is stateful and not thread-safe. A new instance should be use per each desired {@link Predicate}.
 * <p>
 * This builder adds the capability to build a predicate that filters by the {@link EmailFlags} contained in
 * an email returned from an IMAP mailbox
 * <p>
 * The class is also given the &quot;imap-matcher&quot; alias to make it DSL/XML friendly.
 *
 * @since 4.0
 */
@XmlHints(allowTopLevelDefinition = true)
@Alias("imap-matcher")
public class IMAPEmailPredicateBuilder extends BaseEmailPredicateBuilder {

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
  protected Predicate<? extends BaseEmailAttributes> getBasePredicate() {

    Predicate<IMAPEmailAttributes> predicate = imapEmailAttributes -> true;

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

  public Boolean getSeen() {
    return seen;
  }

  public Boolean getAnswered() {
    return answered;
  }

  public Boolean getDeleted() {
    return deleted;
  }

  public Boolean getRecent() {
    return recent;
  }

  public IMAPEmailPredicateBuilder setAnswered(Boolean answered) {
    this.answered = answered;
    return this;
  }

  public IMAPEmailPredicateBuilder setDeleted(Boolean deleted) {
    this.deleted = deleted;
    return this;
  }

  public IMAPEmailPredicateBuilder setRecent(Boolean recent) {
    this.recent = recent;
    return this;
  }

  public IMAPEmailPredicateBuilder setSeen(Boolean seen) {
    this.seen = seen;
    return this;
  }
}
