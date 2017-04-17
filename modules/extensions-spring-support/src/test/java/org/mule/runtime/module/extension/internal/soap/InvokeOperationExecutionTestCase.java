/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.soap;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.services.soap.SoapTestUtils.assertSimilarXml;
import org.mule.functional.junit4.SoapExtensionFunctionalTestCase;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.runtime.core.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.module.extension.internal.soap.services.FootballService;
import org.mule.runtime.module.extension.internal.soap.services.LaLigaService;
import org.mule.service.http.api.HttpService;
import org.mule.services.http.impl.service.HttpServiceImplementation;
import org.mule.services.soap.SoapServiceImplementation;
import org.mule.services.soap.TestHttpSoapServer;
import org.mule.services.soap.api.SoapService;
import org.mule.tck.SimpleUnitTestSupportSchedulerService;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.soap.extension.FootballSoapExtension;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;

public class InvokeOperationExecutionTestCase extends SoapExtensionFunctionalTestCase {

  @Rule
  public DynamicPort footballPort = new DynamicPort("footballPort");

  @Rule
  public DynamicPort laLigaPort = new DynamicPort("laLigaPort");

  private final TestHttpSoapServer footballService = new TestHttpSoapServer(footballPort.getNumber(), new FootballService());
  private final TestHttpSoapServer laLigaService = new TestHttpSoapServer(laLigaPort.getNumber(), new LaLigaService());

  private final SchedulerService schedulerService = new SimpleUnitTestSupportSchedulerService();
  private final HttpService httpService = new HttpServiceImplementation(schedulerService);
  private final SoapService soapService = new SoapServiceImplementation(httpService);

  @Override
  protected void addBuilders(List<ConfigurationBuilder> builders) {
    super.addBuilders(builders);
    try {
      startIfNeeded(httpService);
    } catch (MuleException e) {
      // do nothing
    }
    builders.add(new AbstractConfigurationBuilder() {

      @Override
      protected void doConfigure(MuleContext muleContext) throws Exception {
        muleContext.getRegistry().registerObject(httpService.getName(), httpService);
        muleContext.getRegistry().registerObject(soapService.getName(), soapService);
      }
    });
  }

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    footballService.init();
    laLigaService.init();
    System.setProperty("footballAddress", footballService.getDefaultAddress());
    System.setProperty("laLigaAddress", laLigaService.getDefaultAddress());
  }

  @Override
  protected void doTearDown() throws Exception {
    super.doTearDown();
    footballService.stop();
    laLigaService.stop();
    stopIfNeeded(httpService);
    stopIfNeeded(schedulerService);
  }

  @Override
  protected String getConfigFile() {
    return "soap-config.xml";
  }

  @Override
  protected Class<?>[] getAnnotatedExtensionClasses() {
    return new Class<?>[] {FootballSoapExtension.class};
  }

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


  private String getBodyXml(String tagName, String content) {
    String ns = "http://services.soap.internal.extension.module.runtime.mule.org/";
    return String.format("<con:%s xmlns:con=\"%s\">%s</con:%s>", tagName, ns, content, tagName);
  }
}
