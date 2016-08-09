/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.internal.commands;

import static java.lang.String.format;
import static java.nio.file.Paths.get;
import static javax.mail.Folder.READ_ONLY;
import static org.mule.runtime.core.util.FileUtils.write;
import org.mule.extension.email.internal.retriever.RetrieverConnection;
import org.mule.extension.email.api.exception.EmailException;
import org.mule.extension.email.api.exception.EmailRetrieverException;
import org.mule.runtime.api.message.MuleMessage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;

/**
 * Represents the store emails operation.
 *
 * @since 4.0
 */
public final class StoreCommand {

  private final EmailIdConsumerExecutor executor = new EmailIdConsumerExecutor();

  /**
   * Stores the specified email of id {@code emailId} into the configured {@code localDirectory}.
   * <p>
   * if no emailId is specified, the operation will try to find an email or {@link List} of emails in the incoming
   * {@link MuleMessage}.
   * <p>
   * If no email(s) are found in the {@link MuleMessage} and no {@code emailId} is specified. the operation will fail.
   * <p>
   * The emails are stored as mime message in a ".txt" format.
   * <p>
   * The name of the email file is composed by the subject and the received date of the email.
   *
   * @param connection the associated {@link RetrieverConnection}.
   * @param muleMessage the incoming {@link MuleMessage}.
   * @param folderName the name of the folder where the email(s) is going to be fetched.
   * @param localDirectory the localDirectory where the emails are going to be stored.
   * @param fileName the name of the file that is going to be stored. The operation will append the email number and received date
   *        in the end.
   * @param emailId the optional number of the email to be marked. for default the email is taken from the incoming
   *        {@link MuleMessage}.
   * @param overwrite if should overwrite a file that already exist or not.
   */
  public void store(RetrieverConnection connection, MuleMessage muleMessage, String folderName, String localDirectory,
                    final String fileName, Integer emailId, boolean overwrite) {
    Folder folder = connection.getFolder(folderName, READ_ONLY);
    executor.execute(muleMessage, emailId, id -> {
      try {
        Message message = folder.getMessage(id);
        Path emailFilePath = get(localDirectory, formatEmailFileName(message, fileName));
        File emailFile = emailFilePath.toFile();
        if (emailFile.exists()) {
          if (overwrite) {
            Files.delete(emailFilePath);
            writeContent(message, emailFile);
          }
        } else {
          writeContent(message, emailFile);
        }
      } catch (MessagingException | IOException me) {
        throw new EmailRetrieverException(me);
      }
    });

  }

  /**
   * Writes an email into a file.
   *
   * @param message the email to be stored.
   * @param emailFile the file where the email will be written.
   */
  private void writeContent(Message message, File emailFile) throws IOException, MessagingException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    message.writeTo(outputStream);
    write(emailFile, outputStream.toString());
  }

  /**
   * Formats the name of the email file to be stored, the generated name will follow a {fileName|subject}-emailId_receivedDate
   * format.
   * <p>
   * if the fileName is not specified the emailSubject will be used for it.
   *
   * @param message the message to be stored
   * @param fileName the fileName specified by the user. Can be null.
   * @return a file name in a {fileName|subject}-emailId_receivedDate format.
   */
  private String formatEmailFileName(Message message, String fileName) {
    int messageNumber = message.getMessageNumber();
    try {
      Date receivedDate = message.getReceivedDate() != null ? message.getReceivedDate() : new Date();
      String subject = message.getSubject();
      return format("%s-%s_%s.txt", fileName != null ? fileName : subject, messageNumber, receivedDate);
    } catch (MessagingException e) {
      throw new EmailException(format("Error while formatting email number [%s] file name", messageNumber), e);
    }
  }

}
