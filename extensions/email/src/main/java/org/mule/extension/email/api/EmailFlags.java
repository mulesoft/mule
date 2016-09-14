/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api;

import static javax.mail.Flags.Flag.ANSWERED;
import static javax.mail.Flags.Flag.DELETED;
import static javax.mail.Flags.Flag.DRAFT;
import static javax.mail.Flags.Flag.RECENT;
import static javax.mail.Flags.Flag.SEEN;

import javax.mail.Flags;

/**
 * The {@link EmailFlags} class represents the set of flags on a Message. Flags are composed of predefined system flags that most
 * folder implementations are expected to support.
 *
 * @since 4.0
 */
public class EmailFlags {

  /**
   * Specifies if the email message has been answered or not.
   */
  private final boolean answered;

  /**
   * Specifies if the email message has been deleted or not.
   */
  private final boolean deleted;

  /**
   * Specifies if the email message is a draft or not.
   */
  private final boolean draft;

  /**
   * Specifies if the email message is recent or not.
   */
  private final boolean recent;

  /**
   * Specifies if the email message has been seen or not.
   */
  private final boolean seen;

  public EmailFlags(Flags flags) {
    this.answered = flags.contains(ANSWERED);
    this.deleted = flags.contains(DELETED);
    this.draft = flags.contains(DRAFT);
    this.recent = flags.contains(RECENT);
    this.seen = flags.contains(SEEN);
  }

  /**
   * @return if this message has been answered.
   */
  public boolean isAnswered() {
    return answered;
  }

  /**
   * @return if this message has been isDeleted.
   */
  public boolean isDeleted() {
    return deleted;
  }

  /**
   * @return if this message is a isDraft.
   */
  public boolean isDraft() {
    return draft;
  }

  /**
   * @return if this message is isRecent. Folder implementations set this flag to indicate that this message is new to this
   *         folder, that is, it has arrived since the last time this folder was opened.
   */
  public boolean isRecent() {
    return recent;
  }

  /**
   * @return if this message has been isSeen. This flag is implicitly set by the implementation when the the email content is
   *         returned to the client in some form.
   */
  public boolean isSeen() {
    return seen;
  }

}
