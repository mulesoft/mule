/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api;

import static java.util.regex.Pattern.compile;

import org.mule.runtime.core.api.util.TimeSinceFunction;
import org.mule.runtime.core.api.util.TimeUntilFunction;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;

import java.time.LocalDateTime;
import java.util.function.Predicate;

/**
 * Builds a {@link Predicate} which verifies that a {@link EmailAttributes} instance is compliant with a number of criteria. This
 * builder is stateful and not thread-safe. A new instance should be use per each desired {@link Predicate}.
 * <p>
 * This builder can either be used programmatically or through Mule's SDK since its internal state is annotated with the
 * {@link Parameter} annotation.
 * <p>
 * Criterias are evaluated using an {@code AND} operator, meaning that for the predicate to accept a file, ALL the criterias must
 * be complied with.
 * <p>
 * None of the criteria fields are mandatory. If a particular criteria is not specified, then it's simply not applied on the
 * evaluation.
 * <p>
 * The class is also given the &quot;matcher&quot; alias to make it DSL/XML friendly.
 *
 * @since 4.0
 */
@Alias("matcher")
public class EmailPredicateBuilder {

  private static final TimeUntilFunction TIME_UNTIL = new TimeUntilFunction();
  private static final TimeSinceFunction TIME_SINCE = new TimeSinceFunction();

  /**
   * Indicates since which date the received emails must be retrieved
   */
  @Parameter
  @Optional
  private LocalDateTime receivedSince;

  /**
   * Indicates until which date the received emails must be retrieved
   */
  @Parameter
  @Optional
  private LocalDateTime receivedUntil;

  /**
   * Indicates since which date the sent emails must be retrieved
   */
  @Parameter
  @Optional
  private LocalDateTime sentSince;

  /**
   * Indicates until which date the sent emails must be retrieved
   */
  @Parameter
  @Optional
  private LocalDateTime sentUntil;

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

  /**
   * Subject Regex to match with the wanted emails
   */
  @Parameter
  @Optional
  private String subjectRegex;

  /**
   * From Email Address Regex to match with the wanted emails
   */
  @Parameter
  @Optional
  private String fromRegex;

  /**
   * Builds a {@link Predicate} from the criterias in {@code this} builder's state.
   *
   * @return a {@link Predicate}
   */
  public Predicate<EmailAttributes> build() {
    Predicate<EmailAttributes> predicate = emailAttributes -> true;

    if (subjectRegex != null) {
      Predicate<String> subjectPredicate = compile(subjectRegex).asPredicate();
      predicate = predicate.and(attributes -> subjectPredicate.test(attributes.getSubject()));
    }

    if (fromRegex != null) {
      Predicate<String> fromPredicate = compile(fromRegex).asPredicate();
      predicate = predicate.and(attributes -> attributes.getFromAddresses().stream().anyMatch(fromPredicate));
    }

    if (receivedSince != null) {
      predicate = predicate.and(attributes -> attributes.getReceivedDate() != null
          && TIME_SINCE.apply(receivedSince, attributes.getReceivedDate()));
    }

    if (receivedUntil != null) {
      predicate = predicate.and(attributes -> attributes.getReceivedDate() != null
          && TIME_UNTIL.apply(receivedUntil, attributes.getReceivedDate()));
    }

    if (sentSince != null) {
      predicate =
          predicate.and(attributes -> attributes.getSentDate() != null && TIME_SINCE.apply(sentSince, attributes.getSentDate()));
    }

    if (sentUntil != null) {
      predicate =
          predicate.and(attributes -> attributes.getSentDate() != null && TIME_UNTIL.apply(sentUntil, attributes.getSentDate()));
    }

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

  public EmailPredicateBuilder setAnswered(Boolean answered) {
    this.answered = answered;
    return this;
  }

  public EmailPredicateBuilder setDeleted(Boolean deleted) {
    this.deleted = deleted;
    return this;
  }

  public EmailPredicateBuilder setFromRegex(String fromRegex) {
    this.fromRegex = fromRegex;
    return this;
  }

  public EmailPredicateBuilder setReceivedSince(LocalDateTime receivedSince) {
    this.receivedSince = receivedSince;
    return this;
  }

  public EmailPredicateBuilder setReceivedUntil(LocalDateTime receivedUntil) {
    this.receivedUntil = receivedUntil;
    return this;
  }

  public EmailPredicateBuilder setRecent(Boolean recent) {
    this.recent = recent;
    return this;
  }

  public EmailPredicateBuilder setSeen(Boolean seen) {
    this.seen = seen;
    return this;
  }

  public EmailPredicateBuilder setSubjectRegex(String subjectRegex) {
    this.subjectRegex = subjectRegex;
    return this;
  }

  public EmailPredicateBuilder setSentSince(LocalDateTime sentSince) {
    this.sentSince = sentSince;
    return this;
  }

  public EmailPredicateBuilder setSentUntil(LocalDateTime sentUntil) {
    this.sentUntil = sentUntil;
    return this;
  }
}
