/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api.predicate;

import static java.util.regex.Pattern.compile;
import org.mule.extension.email.api.attributes.BaseEmailAttributes;
import org.mule.runtime.core.api.util.TimeSinceFunction;
import org.mule.runtime.core.api.util.TimeUntilFunction;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;

import java.time.LocalDateTime;
import java.util.function.Predicate;

/**
 * Builds a {@link Predicate} which verifies that a {@link BaseEmailAttributes} instance is compliant with a number of criteria. This
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
 * The class is also given the &quot;default-matcher&quot; alias to make it DSL/XML friendly.
 *
 * @since 4.0
 */
@Alias("default-matcher")
public class DefaultEmailPredicateBuilder {

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

  public DefaultEmailPredicateBuilder setFromRegex(String fromRegex) {
    this.fromRegex = fromRegex;
    return this;
  }

  public DefaultEmailPredicateBuilder setReceivedSince(LocalDateTime receivedSince) {
    this.receivedSince = receivedSince;
    return this;
  }

  public DefaultEmailPredicateBuilder setReceivedUntil(LocalDateTime receivedUntil) {
    this.receivedUntil = receivedUntil;
    return this;
  }

  public DefaultEmailPredicateBuilder setSubjectRegex(String subjectRegex) {
    this.subjectRegex = subjectRegex;
    return this;
  }

  public DefaultEmailPredicateBuilder setSentSince(LocalDateTime sentSince) {
    this.sentSince = sentSince;
    return this;
  }

  public DefaultEmailPredicateBuilder setSentUntil(LocalDateTime sentUntil) {
    this.sentUntil = sentUntil;
    return this;
  }

  protected Predicate<? extends BaseEmailAttributes> getBasePredicate() {
    return emailAttributes -> true;
  }
}
