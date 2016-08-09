/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.compatibility.core.api.config.MuleEndpointProperties;
import org.mule.compatibility.core.api.endpoint.EndpointFactory;
import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.transport.file.ExpressionFilenameParser;
import org.mule.compatibility.transport.file.FileConnector;
import org.mule.compatibility.transport.file.FilenameParser;
import org.mule.compatibility.transport.file.filters.FilenameRegexFilter;
import org.mule.compatibility.transport.file.transformers.FileToByteArray;
import org.mule.compatibility.transport.file.transformers.FileToString;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.routing.filter.Filter;
import org.mule.runtime.core.construct.Flow;

import org.junit.Test;

public class FileNamespaceHandlerTestCase extends FunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "file-namespace-config-flow.xml";
  }

  @Test
  public void testConfig() throws Exception {
    FileConnector c = (FileConnector) muleContext.getRegistry().lookupObject("fileConnector");
    assertNotNull(c);

    assertEquals(1234, c.getFileAge());
    assertEquals("abc", c.getMoveToDirectory());
    assertEquals("bcd", c.getMoveToPattern());
    assertEquals("cde", c.getOutputPattern());
    assertEquals(2345, c.getPollingFrequency());
    assertTrue(getFileInsideWorkingDirectory("readFromDirectory").getAbsolutePath().endsWith(c.getReadFromDirectory()));
    assertTrue(getFileInsideWorkingDirectory("writeToDirectory").getAbsolutePath().endsWith(c.getWriteToDirectory()));
    assertTrue(getFileInsideWorkingDirectory("workDirectory").getAbsolutePath().endsWith(c.getWorkDirectory()));
    assertEquals("#[org.mule.runtime.core.util.UUID.getUUID()]", c.getWorkFileNamePattern());
    assertEquals(false, c.isAutoDelete());
    assertEquals(true, c.isOutputAppend());
    assertEquals(true, c.isSerialiseObjects());
    assertEquals(false, c.isStreaming());
    assertTrue(c.isRecursive());

    // Not implemented yet, see MULE-2671
    // assertNull(c.getComparator());
    // assertFalse(c.isReverseOrder());

    FilenameParser parser = c.getFilenameParser();
    assertTrue(parser.getClass().getName(), c.getFilenameParser() instanceof DummyFilenameParser);

    assertTrue(c.isConnected());
    assertTrue(c.isStarted());
  }

  @Test
  public void testThirdConnector() throws Exception {
    FileConnector c = (FileConnector) muleContext.getRegistry().lookupObject("thirdConnector");
    assertNotNull(c);

    FilenameParser parser = c.getFilenameParser();
    assertTrue(parser.getClass().getName(), c.getFilenameParser() instanceof ExpressionFilenameParser);

    assertTrue(c.isConnected());
    assertTrue(c.isStarted());
    assertFalse(c.isRecursive());
  }

  @Test
  public void testTransformersOnEndpoints() throws Exception {
    Object transformer1 = getEndpointFactory().getInboundEndpoint("ep1").getMessageProcessors().get(0);
    assertNotNull(transformer1);
    assertEquals(FileToByteArray.class, transformer1.getClass());

    Object transformer2 = getEndpointFactory().getInboundEndpoint("ep2").getMessageProcessors().get(0);
    assertNotNull(transformer2);
    assertEquals(FileToString.class, transformer2.getClass());
  }

  @Test
  public void testFileFilter() throws Exception {
    Object flow = muleContext.getRegistry().lookupObject("Test");
    assertNotNull(flow);

    InboundEndpoint endpoint = (InboundEndpoint) ((Flow) flow).getMessageSource();

    Filter filter = endpoint.getFilter();
    assertNotNull(filter);

    assertTrue(filter instanceof FilenameRegexFilter);
    final FilenameRegexFilter f = (FilenameRegexFilter) filter;
    assertEquals(false, f.isCaseSensitive());
    assertEquals("(^SemDirector_Report-\\d)(.*)(tab$)", f.getPattern());
  }

  public EndpointFactory getEndpointFactory() {
    return (EndpointFactory) muleContext.getRegistry().lookupObject(MuleEndpointProperties.OBJECT_MULE_ENDPOINT_FACTORY);
  }
}
