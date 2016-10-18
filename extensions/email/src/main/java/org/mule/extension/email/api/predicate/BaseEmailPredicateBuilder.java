/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api.predicate;

import org.mule.extension.email.api.attributes.BaseEmailAttributes;
import org.mule.runtime.core.api.util.TimeSinceFunction;
import org.mule.runtime.core.api.util.TimeUntilFunction;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;

import java.time.LocalDateTime;
import java.util.function.Predicate;

import static java.util.regex.Pattern.compile;

/**
 * Base builder class for {@link Predicate}s that verify that a {@link BaseEmailAttributes} instance is compliant with a
 * number of criteria. Builder implementation of this class are stateful and not thread-safe. A new instance should be
 * use per each desired {@link Predicate}.
 * <p>
 * Builder implementations can either be used programmatically or through Mule's SDK since its internal state is
 * annotated with the {@link Parameter} annotation.
 * <p>
 * Criterias are evaluated using an {@code AND} operator, meaning that for the predicate to accept a file, ALL the criterias must
 * be complied with.
 * <p>
 * None of the criteria fields are mandatory. If a particular criteria is not specified, then it's simply not applied on the
 * evaluation.
 * <p>
 *
 * @since 4.0
 */
public abstract class BaseEmailPredicateBuilder {

  private static final TimeUntilFunction TIME_UNTIL = new TimeUntilFunction();
  private static final TimeSinceFunction TIME_SINCE = new TimeSinceFunction();

  /**
   * Indicates since which date the received emails must be retrieved
   *
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
  public Predicate<BaseEmailAttributes> build() {
    Predicate<? extends BaseEmailAttributes> predicate = getBasePredicate();

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

    return (Predicate<BaseEmailAttributes>) predicate;
  }

  public LocalDateTime getReceivedSince() {
    return receivedSince;
  }

  public LocalDateTime getReceivedUntil() {
    return receivedUntil;
  }

  public LocalDateTime getSentSince() {
    return sentSince;
  }

  public LocalDateTime getSentUntil() {
    return sentUntil;
  }

  public String getSubjectRegex() {
    return subjectRegex;
  }

  public String getFromRegex() {
    return fromRegex;
  }

  protected Predicate<? extends BaseEmailAttributes> getBasePredicate() {
    return emailAttributes -> true;
  }

  public BaseEmailPredicateBuilder setFromRegex(String fromRegex) {
    this.fromRegex = fromRegex;
    return this;
  }

  public BaseEmailPredicateBuilder setReceivedSince(LocalDateTime receivedSince) {
    this.receivedSince = receivedSince;
    return this;
  }

  public BaseEmailPredicateBuilder setReceivedUntil(LocalDateTime receivedUntil) {
    this.receivedUntil = receivedUntil;
    return this;
  }

  public BaseEmailPredicateBuilder setSubjectRegex(String subjectRegex) {
    this.subjectRegex = subjectRegex;
    return this;
  }

  public BaseEmailPredicateBuilder setSentSince(LocalDateTime sentSince) {
    this.sentSince = sentSince;
    return this;
  }

  public BaseEmailPredicateBuilder setSentUntil(LocalDateTime sentUntil) {
    this.sentUntil = sentUntil;
    return this;
  }
}
