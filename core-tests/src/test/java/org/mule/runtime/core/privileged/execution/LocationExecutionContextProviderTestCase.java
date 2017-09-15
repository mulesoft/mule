/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.execution;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.privileged.execution.LocationExecutionContextProvider.SOURCE_ELEMENT_ANNOTATION_KEY;
import static org.mule.runtime.core.privileged.execution.LocationExecutionContextProvider.getSourceXML;
import org.mule.runtime.api.component.Component;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class LocationExecutionContextProviderTestCase extends AbstractMuleTestCase {

  @Mock
  private Component component;

  @Test
  public void sanitizedUrl() {
    withXmlElement(component, "<sftp:outbound-endpoint url=\"sftp://muletest:muletest@localhost:22198/~/testdir");
    String sanitized = getSourceXML(component);
    assertThat(sanitized, equalTo("<sftp:outbound-endpoint url=\"sftp://<<credentials>>@localhost:22198/~/testdir"));
  }

  @Test
  public void sanitizedAddress() {
    withXmlElement(component, "<sftp:outbound-endpoint address=\"sftp://muletest:muletest@localhost:22198/~/testdir");
    String sanitized = getSourceXML(component);
    assertThat(sanitized, equalTo("<sftp:outbound-endpoint address=\"sftp://<<credentials>>@localhost:22198/~/testdir"));
  }

  @Test
  public void sanitizedPasswordAttribute() {
    withXmlElement(component, "<sftp:config username=\"user\" password=\"pass\" />");
    String sanitized = getSourceXML(component);
    assertThat(sanitized, equalTo("<sftp:config username=\"user\" password=\"<<credentials>>\" />"));

  }

  private void withXmlElement(Component component, String value) {
    when(component.getAnnotation(SOURCE_ELEMENT_ANNOTATION_KEY)).thenReturn(value);
  }
}
