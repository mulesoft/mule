/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transport.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.mule.compatibility.transport.file.FileMessageReceiver;
import org.mule.functional.functional.FunctionalTestComponent;
import org.mule.functional.functional.FunctionalTestNotification;
import org.mule.functional.functional.FunctionalTestNotificationListener;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.DefaultMuleContext;
import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.core.api.context.notification.ServerNotification;
import org.mule.runtime.core.connector.PollingController;
import org.mule.runtime.core.util.FileUtils;
import org.mule.runtime.core.util.IOUtils;
import org.mule.tck.junit4.rule.SystemProperty;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

import org.junit.ClassRule;
import org.junit.Test;

public class FileFunctionalTestCase extends FunctionalTestCase implements FunctionalTestNotificationListener {

  @ClassRule
  public static SystemProperty filePollOnlyOnPrimaryNode =
      new SystemProperty(FileMessageReceiver.MULE_TRANSPORT_FILE_SINGLEPOLLINSTANCE, "true");

  private Object receivedData = null;
  private boolean shouldPoll;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/providers/file/file-config-flow.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    muleContext.registerListener(this);
  }

  @Override
  protected void doTearDown() throws Exception {
    super.doTearDown();
    muleContext.unregisterListener(this);
  }

  @Test
  public void testRelative() throws IOException, InterruptedException {
    // create binary file data to be written
    byte[] data = new byte[100000];
    for (int i = 0; i < data.length; i++) {
      data[i] = (byte) (Math.random() * 128);
    }

    File f = FileUtils.newFile("./test/testfile.temp");
    f.createNewFile();
    FileOutputStream fos = new FileOutputStream(f);
    IOUtils.write(data, fos);
    IOUtils.closeQuietly(fos);

    shouldPoll = false;

    ((DefaultMuleContext) muleContext).setPollingController(new PollingController() {

      @Override
      public boolean isPrimaryPollingInstance() {
        return shouldPoll;
      }
    });
    // atomically rena
    // me the file to make it available for polling
    f.renameTo(FileUtils.newFile(f.getPath().replaceAll(".temp", ".data")));

    // give polling a chance
    Thread.sleep(5000);

    synchronized (this) {
      assertNull(receivedData);
    }

    shouldPoll = true;
    // give polling a chance
    Thread.sleep(5000);
    synchronized (this) {
      assertNotNull(receivedData);
      assertTrue(receivedData instanceof byte[]);
      byte[] receivedBytes = (byte[]) receivedData;
      assertEquals(data.length, receivedBytes.length);
      assertTrue(Arrays.equals(data, receivedBytes));
    }
  }

  @Override
  public void onNotification(ServerNotification notification) {
    synchronized (this) {
      logger.debug("received notification: " + notification);
      // save the received message data for verification
      this.receivedData = ((FunctionalTestNotification) notification).getReplyMessage();
    }
  }

  public static class FileTestComponent extends FunctionalTestComponent {

    @Override
    public Object onCall(MuleEventContext context) throws Exception {
      // there should not be any transformers configured by default, so the
      // return message should be a byte[]
      super.setReturnData(context.getMessage().getPayload());
      return super.onCall(context);
    }
  }
}
