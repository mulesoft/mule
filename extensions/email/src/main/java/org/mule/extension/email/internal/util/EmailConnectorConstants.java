/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.util;

/**
 * this class contains common methods for email handling.
 *
 * @since 4.0
 */
public final class EmailConnectorConstants {

  /**
   * Default folder name for all the mailboxes.
   */
  public static final String INBOX_FOLDER = "INBOX";

  /**
   * defines all the multipart content types
   */
  public static final String MULTIPART = "multipart/*";

  /**
   * defines all the text content types
   */
  public static final String TEXT = "text/*";

  /**
   * Default email content type
   */
  public static final String TEXT_PLAIN = "text/plain";

  /**
   * Default port value for SMTP servers.
   */
  public static final String SMTP_PORT = "25";

  /**
   * Default port value for SMTPS servers.
   */
  public static final String SMTPS_PORT = "465";

  /**
   * Default port value for POP3 servers.
   */
  public static final String POP3_PORT = "110";

  /**
   * Default port value for POP3S servers.
   */
  public static final String POP3S_PORT = "995";

  /**
   * Default port value for IMAP servers.
   */
  public static final String IMAP_PORT = "143";

  /**
   * Default port value for IMAPS servers.
   */
  public static final String IMAPS_PORT = "993";

  /**
   * The content type header name.
   */
  public static final String CONTENT_TYPE_HEADER = "Content-Type";

  /**
   * Hide constructor
   */
  private EmailConnectorConstants() {}
}
