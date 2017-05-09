/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api.predicate;

import static org.mule.extension.email.api.predicate.EmailFilterPolicy.INCLUDE;
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
  @Optional(defaultValue = "INCLUDE")
  private EmailFilterPolicy seen;

  /**
   * Indicates if should retrieve 'answered' or 'not answered' emails
   */
  @Parameter
  @Optional(defaultValue = "INCLUDE")
  private EmailFilterPolicy answered;

  /**
   * Indicates if should retrieve 'marked as deleted' or 'not marked as deleted' emails
   */
  @Parameter
  @Optional(defaultValue = "INCLUDE")
  private EmailFilterPolicy deleted;

  /**
   * "Indicates if should retrieve 'recent' or 'not recent' emails
   */
  @Parameter
  @Optional(defaultValue = "INCLUDE")
  private EmailFilterPolicy recent;

  @Override
  protected Predicate<? extends BaseEmailAttributes> getBasePredicate() {

    Predicate<IMAPEmailAttributes> predicate = imapEmailAttributes -> true;

    if (!INCLUDE.equals(recent)) {
      predicate = predicate.and(attributes -> recent.asBoolean().get() == attributes.getFlags().isRecent());
    }

    if (!INCLUDE.equals(deleted)) {
      predicate = predicate.and(attributes -> deleted.asBoolean().get() == attributes.getFlags().isDeleted());
    }

    if (!INCLUDE.equals(answered)) {
      predicate = predicate.and(attributes -> answered.asBoolean().get() == attributes.getFlags().isAnswered());
    }

    if (!INCLUDE.equals(seen)) {
      predicate = predicate.and(attributes -> seen.asBoolean().get() == attributes.getFlags().isSeen());
    }

    return predicate;
  }

  public EmailFilterPolicy getSeen() {
    return seen;
  }

  public EmailFilterPolicy getAnswered() {
    return answered;
  }

  public EmailFilterPolicy getDeleted() {
    return deleted;
  }

  public EmailFilterPolicy getRecent() {
    return recent;
  }

  public IMAPEmailPredicateBuilder setAnswered(EmailFilterPolicy answered) {
    this.answered = answered;
    return this;
  }

  public IMAPEmailPredicateBuilder setDeleted(EmailFilterPolicy deleted) {
    this.deleted = deleted;
    return this;
  }

  public IMAPEmailPredicateBuilder setRecent(EmailFilterPolicy recent) {
    this.recent = recent;
    return this;
  }

  public IMAPEmailPredicateBuilder setSeen(EmailFilterPolicy seen) {
    this.seen = seen;
    return this;
  }
}
