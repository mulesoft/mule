/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.email.retriever;

import static javax.mail.Folder.READ_ONLY;
import static javax.mail.Folder.READ_WRITE;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mule.extension.email.internal.AbstractEmailConnection.PASSWORD_NO_USERNAME_ERROR;
import static org.mule.extension.email.internal.AbstractEmailConnection.USERNAME_NO_PASSWORD_ERROR;
import static org.mule.extension.email.internal.EmailProtocol.IMAP;
import static org.mule.extension.email.internal.util.EmailConnectorUtils.INBOX_FOLDER;
import static org.mule.extension.email.util.EmailTestUtils.JUANI_EMAIL;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import org.mule.extension.email.internal.retriever.RetrieverConnection;
import org.mule.extension.email.api.exception.EmailConnectionException;
import org.mule.extension.email.api.exception.EmailException;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Session.class)
@PowerMockIgnore("javax.management.*")
public class RetrieverConnectionTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private static final String RECENT_FOLDER = "Recent";

  private Store store;
  private RetrieverConnection connection;

  @Before
  public void setUpTestConnection() throws Exception {
    store = mock(Store.class);
    doNothing().when(store).connect(anyString(), anyString());

    TestFolder inbox = new TestFolder(store, INBOX_FOLDER);
    when(store.getFolder(INBOX_FOLDER)).thenReturn(inbox);

    TestFolder recent = new TestFolder(store, RECENT_FOLDER);
    when(store.getFolder(RECENT_FOLDER)).thenReturn(recent);

    PowerMockito.mockStatic(Session.class);
    Session session = mock(Session.class);
    when(session.getStore(anyString())).thenReturn(store);
    when(Session.getInstance(anyObject(), anyObject())).thenReturn(session);

    connection = new RetrieverConnection(IMAP, JUANI_EMAIL, "password", "127.0.0.1", "123", 1000, 1000, 1000, null);
  }

  @Test
  public void getFolder() {
    Folder folder = connection.getFolder(INBOX_FOLDER, READ_WRITE);
    assertFolder(folder, INBOX_FOLDER, READ_WRITE);
  }

  @Test
  public void changeFolder() throws MessagingException {
    getFolder();
    Folder recent = connection.getFolder(RECENT_FOLDER, READ_WRITE);
    assertFolder(recent, RECENT_FOLDER, READ_WRITE);
    assertThat(store.getFolder(INBOX_FOLDER).isOpen(), is(false));
  }

  @Test
  public void getSameFolder() throws MessagingException {
    getFolder();
    getFolder();
  }

  @Test
  public void changeFolderMode() throws MessagingException {
    getFolder();
    Folder inbox = connection.getFolder(INBOX_FOLDER, READ_ONLY);
    assertFolder(inbox, INBOX_FOLDER, READ_ONLY);
  }

  @Test
  public void usernameMissingPassword() throws MessagingException, EmailConnectionException {
    expectedException.expect(EmailException.class);
    expectedException.expectMessage(is(PASSWORD_NO_USERNAME_ERROR));
    new RetrieverConnection(IMAP, null, "password", "127.0.0.1", "123", 1000, 1000, 1000, null);
  }

  @Test
  public void passwordMissingUsername() throws MessagingException, EmailConnectionException {
    expectedException.expect(EmailException.class);
    expectedException.expectMessage(is(USERNAME_NO_PASSWORD_ERROR));
    new RetrieverConnection(IMAP, JUANI_EMAIL, null, "127.0.0.1", "123", 1000, 1000, 1000, null);
  }

  private void assertFolder(Folder folder, String name, int mode) {
    assertThat(folder, is(not(nullValue())));
    assertThat(folder.getName(), is(name));
    assertThat(folder.getMode(), is(mode));
  }

  private class TestFolder extends Folder {

    private final String folderName;
    private boolean isOpen;

    public TestFolder(Store store, String folderName) {
      super(store);
      this.folderName = folderName;
      this.isOpen = false;
    }

    @Override
    public String getName() {
      return folderName;
    }

    @Override
    public String getFullName() {
      return folderName;
    }

    @Override
    public Folder getParent() throws MessagingException {
      return null;
    }

    @Override
    public boolean exists() throws MessagingException {
      return false;
    }

    @Override
    public Folder[] list(String pattern) throws MessagingException {
      return new Folder[0];
    }

    @Override
    public char getSeparator() throws MessagingException {
      return 0;
    }

    @Override
    public int getType() throws MessagingException {
      return 0;
    }

    @Override
    public boolean create(int type) throws MessagingException {
      return false;
    }

    @Override
    public boolean hasNewMessages() throws MessagingException {
      return false;
    }

    @Override
    public Folder getFolder(String name) throws MessagingException {
      return null;
    }

    @Override
    public boolean delete(boolean recurse) throws MessagingException {
      return false;
    }

    @Override
    public boolean renameTo(Folder f) throws MessagingException {
      return false;
    }

    @Override
    public void open(int mode) throws MessagingException {
      this.isOpen = true;
      this.mode = mode;
    }

    @Override
    public void close(boolean expunge) throws MessagingException {
      if (!isOpen) {
        throw new MessagingException("Cannot close: Folder is already closed");
      }
      this.isOpen = false;
    }

    @Override
    public boolean isOpen() {
      return isOpen;
    }

    @Override
    public Flags getPermanentFlags() {
      return null;
    }

    @Override
    public int getMessageCount() throws MessagingException {
      return 0;
    }

    @Override
    public Message getMessage(int msgnum) throws MessagingException {
      return null;
    }

    @Override
    public void appendMessages(Message[] msgs) throws MessagingException {

    }

    @Override
    public Message[] expunge() throws MessagingException {
      return new Message[0];
    }
  }

}
