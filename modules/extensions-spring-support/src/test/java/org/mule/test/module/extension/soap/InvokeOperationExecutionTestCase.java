/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.soap;

import static org.mule.services.soap.SoapTestUtils.assertSimilarXml;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.util.IOUtils;

import java.io.ByteArrayInputStream;

import org.junit.Test;

public class InvokeOperationExecutionTestCase extends SoapExtensionArtifactFunctionalTestCase {

  @Test
  public void simpleNoParamsOperation() throws Exception {
    Message message = flowRunner("getLeagues").withPayload(getBodyXml("getLeagues", "")).keepStreamsOpen().run().getMessage();
    String response = getBodyXml("getLeaguesResponse", "<league>Calcio</league><league>La Liga</league>");
    assertSimilarXml(response, IOUtils.toString((CursorStreamProvider) message.getPayload().getValue()));
  }

  @Test
  public void operationWithHeaders() throws Exception {
    String requestBody = getBodyXml("getLeagueTeams", "<name>La Liga</name>");
    Message message = flowRunner("getLeagueTeams").withPayload(requestBody).keepStreamsOpen().run().getMessage();
    String response = getBodyXml("getLeagueTeamsResponse", "<team>Barcelona</team><team>Real Madrid</team><team>Atleti</team>");
    assertSimilarXml(response, IOUtils.toString((CursorStreamProvider) message.getPayload().getValue()));
  }

  @Test
  public void uploadAttachment() throws Exception {
    Message message = flowRunner("uploadResult")
        .withPayload(getBodyXml("uploadResult", ""))
        .withVariable("att", new ByteArrayInputStream("Barcelona Won".getBytes()))
        .withVariable("attCt", MediaType.HTML)
        .keepStreamsOpen()
        .run().getMessage();
    String response = getBodyXml("uploadResultResponse", "<message>Ok</message>");
    assertSimilarXml(response, IOUtils.toString((CursorStreamProvider) message.getPayload().getValue()));
  }
}
