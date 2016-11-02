/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.generator;

import static java.util.Collections.emptyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.extension.ws.WscTestUtils.ECHO;
import static org.mule.extension.ws.WscTestUtils.FAIL;
import static org.mule.extension.ws.WscTestUtils.NO_PARAMS;
import static org.mule.extension.ws.WscTestUtils.assertSimilarXml;
import static org.mule.extension.ws.WscTestUtils.getRequestResource;
import org.mule.extension.ws.AbstracWscUnitTestCase;
import org.mule.extension.ws.api.exception.WscException;
import org.mule.extension.ws.internal.introspection.WsdlIntrospecter;

import javax.wsdl.BindingInput;
import javax.wsdl.BindingOperation;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Web Service Consumer")
@Stories("Request Generation")
public class EmptyRequestGeneratorTestCase extends AbstracWscUnitTestCase {

  @Rule
  public ExpectedException exception = ExpectedException.none();
  private EmptyRequestGenerator generator = new EmptyRequestGenerator();

  @Test
  @Description("Checks the generation of a body request for an operation that don't require any parameters")
  public void noParams() throws Exception {
    String request = generator.generateRequest(serviceIntrospecter, operationsTypeLoader, NO_PARAMS);
    assertSimilarXml(request, getRequestResource(NO_PARAMS));
  }

  @Test
  @Description("Checks that the generation of a body request for an operation that require parameters fails")
  public void withParams() throws Exception {
    exception.expect(WscException.class);
    exception.expectMessage("Cannot build default body request for operation [echo], the operation requires input parameters");
    generator.generateRequest(serviceIntrospecter, operationsTypeLoader, ECHO);
  }

  @Test
  @Description("Checks that the generation of a body request for an operation without a body part fails")
  public void noBodyPart() throws Exception {
    exception.expect(WscException.class);
    exception.expectMessage("No SOAP body defined in the WSDL for the specified operation");

    // Makes that the introspecter returns an Binding Operation without input SOAP body.
    WsdlIntrospecter introspecter = mock(WsdlIntrospecter.class);
    BindingOperation bop = mock(BindingOperation.class);
    BindingInput bi = mock(BindingInput.class);
    when(bi.getExtensibilityElements()).thenReturn(emptyList());
    when(bop.getBindingInput()).thenReturn(bi);
    when(introspecter.getBindingOperation(anyString())).thenReturn(bop);

    generator.generateRequest(introspecter, operationsTypeLoader, FAIL);
  }
}
