/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.execution;

import static java.util.Collections.emptyMap;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.privileged.execution.LocationExecutionContextProvider.addMetadataAnnotationsFromDocAttributes;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.Component;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.junit.Test;

@SmallTest
public class LocationExecutionContextProviderTestCase extends AbstractMuleTestCase {

  private final Component component = new AbstractComponent() {};

  @Test
  public void sanitizedUrl() {
    withXmlElement(component, "<sftp:outbound-endpoint url=\"sftp://muletest:muletest@localhost:22198/~/testdir");
    String sanitized = component.getDslSource();
    assertThat(sanitized, equalTo("<sftp:outbound-endpoint url=\"sftp://<<credentials>>@localhost:22198/~/testdir"));
  }

  @Test
  public void sanitizedUrlWithSpecialChars() {
    withXmlElement(component, "<sftp:outbound-endpoint url=\"sftp://muletest:****@localhost:22198/~/testdir");
    String sanitized = component.getDslSource();
    assertThat(sanitized, equalTo("<sftp:outbound-endpoint url=\"sftp://<<credentials>>@localhost:22198/~/testdir"));
  }

  @Test
  public void sanitizedAddress() {
    withXmlElement(component, "<sftp:outbound-endpoint address=\"sftp://muletest:muletest@localhost:22198/~/testdir");
    String sanitized = component.getDslSource();
    assertThat(sanitized, equalTo("<sftp:outbound-endpoint address=\"sftp://<<credentials>>@localhost:22198/~/testdir"));
  }

  @Test
  public void sanitizedPasswordAttribute() {
    withXmlElement(component, "<sftp:config username=\"user\" password=\"pass\" />");
    String sanitized = component.getDslSource();
    assertThat(sanitized, equalTo("<sftp:config username=\"user\" password=\"<<credentials>>\" />"));
  }

  @Test
  public void sanitizedPasswordAttributeWithSpecialChars() {
    withXmlElement(component, "<sftp:config username=\"user\" password=\"****\" />");
    String sanitized = component.getDslSource();
    assertThat(sanitized, equalTo("<sftp:config username=\"user\" password=\"<<credentials>>\" />"));
  }

  @Test
  public void nonLiteralPasswordNotMasked() {
    String xmlDWGeneratedPassword =
        "<http:body ><![CDATA[#[\"grant_type=password&client_id=mt2-web-ui&username=\" ++ dw::core::URL::encodeURIComponent(vars.keycloakUsername!) ++ \"&password=\" ++ dw::core::URL::encodeURIComponent(vars.keycloakPassword!)]]]>\\"
            +
            "</http:body>\n<http:headers><![CDATA[#[output application/java --- {\"Accept\" : \"application/json\",\"Content-Type\" : \"application/x-www-form-urlencoded\"]]>\\"
            +
            "</http:headers>";
    withXmlElement(component, xmlDWGeneratedPassword);
    String sanitized = component.getDslSource();
    assertThat(sanitized, equalTo(xmlDWGeneratedPassword));
  }


  private void withXmlElement(Component component, String value) {
    final Map<QName, Object> annotations = new HashMap<>();
    addMetadataAnnotationsFromDocAttributes(annotations, value, emptyMap());

    component.setAnnotations(annotations);
  }
}
