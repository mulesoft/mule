/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.compatibility.core.api.endpoint.EndpointBuilder;
import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.core.endpoint.EndpointURIEndpointBuilder;
import org.mule.functional.functional.FunctionalTestComponent;
import org.mule.functional.transformer.NoActionTransformer;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.component.DefaultJavaComponent;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.object.SingletonObjectFactory;
import org.mule.runtime.core.transformer.AbstractMessageTransformer;
import org.mule.runtime.core.util.concurrent.Latch;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;

public class FileReceiverMoveDeleteTestCase extends AbstractFileMoveDeleteTestCase {

  @Test
  @Ignore("MULE-6926: flaky test")
  public void testMoveAndDeleteStreaming() throws Exception {
    File inFile = initForRequest();

    File moveToDir = configureConnector(inFile, true, true, true, null);

    assertRecevied(configureService(inFile, true, false));
    assertFiles(inFile, moveToDir, true, true);
  }

  @Test
  public void testMoveAndDeleteWorkDirStreaming() throws Exception {
    File inFile = initForRequest();

    File moveToDir = configureConnector(inFile, true, true, true, true, null);

    assertRecevied(configureService(inFile, true, false));
    assertFiles(inFile, moveToDir, true, true);
  }

  @Test
  @Ignore("MULE-6926: flaky test")
  public void testMoveOnlyStreaming() throws Exception {
    File inFile = initForRequest();

    File moveToDir = configureConnector(inFile, true, true, false, null);

    assertRecevied(configureService(inFile, true, false));
    assertFiles(inFile, moveToDir, true, false);
  }

  @Test
  public void testMoveOnlyWorkDirStreaming() throws Exception {
    File inFile = initForRequest();

    File moveToDir = configureConnector(inFile, true, true, false, true, null);

    assertRecevied(configureService(inFile, true, false));
    assertFiles(inFile, moveToDir, true, false);
  }

  @Test
  public void testDeleteOnlyStreaming() throws Exception {
    File inFile = initForRequest();

    File moveToDir = configureConnector(inFile, true, false, true, null);

    assertRecevied(configureService(inFile, true, false));
    assertFiles(inFile, moveToDir, false, true);
  }

  @Test
  @Ignore("MULE-6926: flaky test")
  public void testNoMoveNoDeleteStreaming() throws Exception {
    File inFile = initForRequest();

    File moveToDir = configureConnector(inFile, true, false, false, null);

    assertRecevied(configureService(inFile, true, false));
    assertFiles(inFile, moveToDir, false, false);
  }

  @Test
  @Ignore("MULE-6926: flaky test")
  public void testMoveAndDelete() throws Exception {
    File inFile = initForRequest();

    File moveToDir = configureConnector(inFile, false, true, true, null);

    assertRecevied(configureService(inFile, false, false));
    assertFiles(inFile, moveToDir, true, true);
  }

  @Test
  public void testMoveOnly() throws Exception {
    File inFile = initForRequest();

    File moveToDir = configureConnector(inFile, false, true, false, null);

    assertRecevied(configureService(inFile, false, false));
    assertFiles(inFile, moveToDir, true, false);
  }

  @Test
  @Ignore("MULE-6926: flaky test")
  public void testDeleteOnly() throws Exception {
    File inFile = initForRequest();

    File moveToDir = configureConnector(inFile, false, false, true, null);

    assertRecevied(configureService(inFile, false, false));
    assertFiles(inFile, moveToDir, false, true);
  }

  @Test
  public void testNoMoveNoDelete() throws Exception {
    File inFile = initForRequest();

    File moveToDir = configureConnector(inFile, false, false, false, null);

    assertRecevied(configureService(inFile, false, false));
    assertFiles(inFile, moveToDir, false, false);
  }

  @Test
  @Ignore("MULE-6926: flaky test")
  public void testMoveAndDeleteFilePayload() throws Exception {
    File inFile = initForRequest();

    File moveToDir = configureConnector(inFile, false, true, false, FileMuleMessageFactory.class);

    assertRecevied(configureService(inFile, false, true));
    assertFiles(inFile, moveToDir, true, true);
  }

  @Test
  public void testMoveOnlyFilePayload() throws Exception {
    File inFile = initForRequest();

    File moveToDir = configureConnector(inFile, false, true, false, FileMuleMessageFactory.class);

    assertRecevied(configureService(inFile, false, true));
    assertFiles(inFile, moveToDir, true, false);
  }

  @Test
  public void testDeleteOnlyFilePayload() throws Exception {
    File inFile = initForRequest();

    File moveToDir = configureConnector(inFile, false, false, true, FileMuleMessageFactory.class);

    assertRecevied(configureService(inFile, false, true));

    assertFiles(inFile, moveToDir, false, true);
  }

  @Test
  @Ignore("MULE-6926: flaky test")
  public void testNoMoveNoDeleteFilePayload() throws Exception {
    File inFile = initForRequest();

    File moveToDir = configureConnector(inFile, false, false, false, FileMuleMessageFactory.class);
    assertRecevied(configureService(inFile, false, true));

    assertFiles(inFile, moveToDir, false, false);
  }

  protected Latch configureService(File inFile, boolean streaming, boolean filePayload) throws Exception {
    Flow flow = new Flow("moveDeleteBridgeService", muleContext);
    String url = fileToUrl(inFile.getParentFile()) + "?connector=moveDeleteConnector";
    Transformer transformer = null;
    if (streaming) {
      if (filePayload) {
        fail("Inconsistant test case: streaming and file payload are not compatible");
      } else {
        transformer = new FileMessageFactoryAssertingTransformer(ReceiverFileInputStream.class);
      }
    } else {
      if (filePayload) {
        transformer = new FileMessageFactoryAssertingTransformer(File.class);
      } else {
        transformer = new FileMessageFactoryAssertingTransformer(byte[].class);
      }
    }

    EndpointBuilder endpointBuilder = new EndpointURIEndpointBuilder(url, muleContext);
    endpointBuilder.addMessageProcessor(transformer);
    if (filePayload) {
      endpointBuilder.addMessageProcessor(new NoActionTransformer());
    }
    InboundEndpoint endpoint = getEndpointFactory().getInboundEndpoint(endpointBuilder);
    flow.setMessageSource(endpoint);

    final Latch latch = new Latch();
    FunctionalTestComponent testComponent = new FunctionalTestComponent();
    testComponent.setMuleContext(muleContext);
    testComponent.setEventCallback((context, message) -> {
      assertEquals(1, latch.getCount());
      assertEquals(TEST_MESSAGE, context.transformMessageToString());
      latch.countDown();
    });
    testComponent.initialise();

    final DefaultJavaComponent component = new DefaultJavaComponent(new SingletonObjectFactory(testComponent));
    component.setMuleContext(muleContext);
    flow.setMessageProcessors(new ArrayList<MessageProcessor>());
    flow.getMessageProcessors().add(component);
    muleContext.getRegistry().registerFlowConstruct(flow);
    return latch;
  }

  protected void assertRecevied(Latch latch) throws Exception {
    assertNotNull(latch);
    assertTrue(latch.await(2000L, TimeUnit.MILLISECONDS));
  }

  private class FileMessageFactoryAssertingTransformer extends AbstractMessageTransformer {

    private Class<?> expectedPayload;

    public FileMessageFactoryAssertingTransformer(Class<?> expectedPayload) {
      this.expectedPayload = expectedPayload;
    }

    @Override
    public Object transformMessage(MuleEvent event, Charset outputEncoding) {
      assertEquals(expectedPayload, event.getMessage().getDataType().getType());

      // If we are streaming, copy/delete shouldn't have happened yet
      if (expectedPayload.equals(ReceiverFileInputStream.class)) {
        File file = ((ReceiverFileInputStream) event.getMessage().getPayload()).getCurrentFile();
        assertFilesUntouched(file);
      }
      return event.getMessage();
    }
  }

}
