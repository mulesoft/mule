/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.api.exception;

import static org.mule.extension.email.api.exception.EmailErrors.ACCESSING_FOLDER;
import org.mule.extension.email.internal.mailbox.MailboxConnection;
import org.mule.extension.email.internal.mailbox.imap.IMAPOperations;
import org.mule.runtime.extension.api.annotation.error.ErrorTypeProvider;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

/**
 * Errors that can be thrown in the {@link IMAPOperations#expungeFolder(MailboxConnection, String)} operation.
 * 
 * @since 4.0
 */
public class EmailAccessingFolderErrorTypeProvider implements ErrorTypeProvider {

  @Override
  public Set<ErrorTypeDefinition> getErrorTypes() {
    return ImmutableSet.<ErrorTypeDefinition>builder()
        .add(ACCESSING_FOLDER)
        .build();
  }
}

