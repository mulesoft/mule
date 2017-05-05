/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.util;

import org.mule.extension.email.api.EmailAttachment;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.util.List;

/**
 * Container group for the {@link EmailAttachment}s
 *
 * @since 4.0
 */
public final class AttachmentsGroup {

  @Optional
  @Parameter
  @Content
  @NullSafe
  private List<EmailAttachment> attachments;

  /**
   * @return a {@link List} of attachments configured in the built outgoing email.
   */
  public List<EmailAttachment> getAttachments() {
    return attachments;
  }

  public void setAttachments(List<EmailAttachment> attachments) {
    this.attachments = attachments;
  }
}
