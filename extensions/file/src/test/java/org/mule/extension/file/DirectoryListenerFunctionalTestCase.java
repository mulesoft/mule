/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file;

import static org.apache.commons.io.FileUtils.write;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.mule.extension.file.api.FileEventType.CREATE;
import static org.mule.extension.file.api.FileEventType.DELETE;
import static org.mule.extension.file.api.FileEventType.UPDATE;
import static org.mule.runtime.core.util.FileUtils.deleteTree;
import org.mule.extension.file.api.FileEventType;
import org.mule.extension.file.api.ListenerFileAttributes;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.el.context.MessageContext;
import org.mule.runtime.module.extension.internal.runtime.source.ExtensionMessageSource;
import org.mule.tck.probe.JUnitLambdaProbe;
import org.mule.tck.probe.PollingProber;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class DirectoryListenerFunctionalTestCase extends FileConnectorTestCase {

  private static final String MATCHERLESS_LISTENER_FOLDER_NAME = "matcherless";
  private static final String WITH_MATCHER_FOLDER_NAME = "withMatcher";
  private static final String CREATED_FOLDER_NAME = "createdFolder";
  private static final String WATCH_FILE = "watchme.txt";
  private static final String WATCH_CONTENT = "who watches the watchmen?";
  private static final String DR_MANHATTAN = "Dr. Manhattan";
  private static final String MATCH_FILE = "matchme.txt";
  private static final int TIMEOUT_MILLIS = 5000;
  private static final int POLL_DELAY_MILLIS = 100;

  private static List<Message> receivedMessages;

  private File matcherLessFolder;
  private File withMatcherFolder;
  private String listenerFolder;

  @Override
  protected String getConfigFile() {
    return "directory-listener-config.xml";
  }

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    super.doSetUpBeforeMuleContextCreation();
    temporaryFolder.newFolder(MATCHERLESS_LISTENER_FOLDER_NAME);
    temporaryFolder.newFolder(WITH_MATCHER_FOLDER_NAME);
    listenerFolder = Paths.get(temporaryFolder.getRoot().getAbsolutePath(), MATCHERLESS_LISTENER_FOLDER_NAME).toString();
    matcherLessFolder = new File(listenerFolder, CREATED_FOLDER_NAME);
    withMatcherFolder = Paths.get(temporaryFolder.getRoot().getAbsolutePath(), WITH_MATCHER_FOLDER_NAME).toFile();
    receivedMessages = new CopyOnWriteArrayList<>();
  }

  @Override
  protected void doTearDown() throws Exception {
    receivedMessages = null;
  }

  @Test
  public void onFileCreated() throws Exception {
    final File file = new File(listenerFolder, WATCH_FILE);
    write(file, WATCH_CONTENT);
    assertEvent(listen(CREATE, file), WATCH_CONTENT);
  }

  @Test
  public void stopAndRestart() throws Exception {
    muleContext.getRegistry().lookupObjects(Flow.class).forEach(flow -> {
      try {
        flow.stop();
        flow.start();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });

    onFileCreated();
  }

  @Test
  public void onFileUpdated() throws Exception {
    onFileCreated();

    final String appendedContent = "\nNOBODY";
    final File file = new File(listenerFolder, WATCH_FILE);

    write(file, appendedContent, true);
    assertEvent(listen(UPDATE, file), WATCH_CONTENT + appendedContent);
  }

  @Test
  public void onFileDeleted() throws Exception {
    onFileCreated();

    final File file = new File(listenerFolder, WATCH_FILE);
    file.delete();
    assertEvent(listen(DELETE, file), null);
  }

  @Test
  public void onDirectoryCreated() throws Exception {
    matcherLessFolder.mkdir();
    assertEvent(listen(CREATE, matcherLessFolder), null);

    final File file = new File(matcherLessFolder, WATCH_FILE);
    write(file, WATCH_CONTENT);
    assertEvent(listen(CREATE, file), WATCH_CONTENT);
  }

  @Test
  public void onDirectoryDeleted() throws Exception {
    onDirectoryCreated();
    deleteTree(matcherLessFolder);
    assertEvent(listen(DELETE, matcherLessFolder), null);
  }

  @Test
  public void onDirectoryRenamed() throws Exception {
    onDirectoryCreated();
    final String updatedName = CREATED_FOLDER_NAME + "twist";

    final File renamedDirectory = new File(listenerFolder, updatedName);
    Files.move(matcherLessFolder.toPath(), renamedDirectory.toPath());

    assertEvent(listen(DELETE, matcherLessFolder), null);
    assertEvent(listen(CREATE, renamedDirectory), null);
  }

  @Test
  public void onDeleteFileAtSubfolder() throws Exception {
    onDirectoryCreated();
    deleteTree(matcherLessFolder);
    assertEvent(listen(DELETE, matcherLessFolder), null);
  }

  @Test
  public void matcher() throws Exception {
    final File file = new File(withMatcherFolder, MATCH_FILE);
    write(file, "");
    Message message = listen(CREATE, file);

    assertThat(message.getPayload().getValue(), equalTo(DR_MANHATTAN));
  }

  @Test
  public void stop() throws Exception {
    muleContext.getRegistry().lookupObjects(ExtensionMessageSource.class).forEach(source -> {
      try {
        source.stop();
      } catch (MuleException e) {
        throw new RuntimeException(e);
      }
    });

    PollingProber prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);
    prober.check(new JUnitLambdaProbe(() -> {
      try {
        onFileCreated();
        return false;
      } catch (Throwable e) {
        return true;
      }
    }, "source did not stop"));
  }

  private void assertEvent(Message message, Object expectedContent) throws Exception {
    Object payload = message.getPayload().getValue();
    if (payload instanceof InputStream) {
      payload = IOUtils.toString((InputStream) payload);
      assertThat((String) payload, not(containsString(DR_MANHATTAN)));
    }

    assertThat(payload, equalTo(expectedContent));
  }

  private Message listen(FileEventType type, File file) {
    PollingProber prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);
    Reference<Message> messageHolder = new Reference<>();
    prober.check(new JUnitLambdaProbe(() -> {
      for (Message message : receivedMessages) {
        ListenerFileAttributes attributes = (ListenerFileAttributes) message.getAttributes();
        if (attributes.getPath().equals(file.getAbsolutePath()) && attributes.getEventType().equals(type.name())) {
          messageHolder.set(message);
          return true;
        }
      }

      return false;
    }));

    return messageHolder.get();
  }

  public static void onMessage(MessageContext messageContext) {
    Object payload = messageContext.getPayload() != null ? messageContext.getPayload() : null;
    Message message = Message.builder().payload(payload).mediaType(messageContext.getDataType().getMediaType())
        .attributes(messageContext.getAttributes()).build();
    receivedMessages.add(message);
  }
}
