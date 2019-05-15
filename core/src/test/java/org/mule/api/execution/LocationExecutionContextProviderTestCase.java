/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.execution;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.mule.api.execution.LocationExecutionContextProvider.SOURCE_ELEMENT_ANNOTATION_KEY;
import static org.mule.api.execution.LocationExecutionContextProvider.getSourceXML;
import org.mule.api.AnnotatedObject;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class LocationExecutionContextProviderTestCase extends AbstractMuleTestCase
{

    @Mock
    private AnnotatedObject annotatedObject;

    @Test
    public void sanitizedUrl()
    {
        withXmlElement(annotatedObject, "<sftp:outbound-endpoint url=\"sftp://muletest:muletest@localhost:22198/~/testdir");
        String sanitized = getSourceXML(annotatedObject);
        assertThat(sanitized, equalTo("<sftp:outbound-endpoint url=\"sftp://<<credentials>>@localhost:22198/~/testdir"));
    }

    @Test
    public void sanitizedAddress()
    {
        withXmlElement(annotatedObject, "<sftp:outbound-endpoint address=\"sftp://muletest:muletest@localhost:22198/~/testdir");
        String sanitized = getSourceXML(annotatedObject);
        assertThat(sanitized, equalTo("<sftp:outbound-endpoint address=\"sftp://<<credentials>>@localhost:22198/~/testdir"));
    }

    @Test
    public void sanitizedPasswordAttribute()
    {
        withXmlElement(annotatedObject, "<sftp:config username=\"user\" password=\"pass\" />");
        String sanitized = getSourceXML(annotatedObject);
        assertThat(sanitized, equalTo("<sftp:config username=\"user\" password=\"<<credentials>>\" />"));

    }
    
    @Test
    public void sanitizedPasswordAttributeWhenUsingRegExpCharactersInPassword()
    {
        String [] specialCharacters = new String[]{"^", ")"};
        String input = "<sftp:config username=\"user\" password=\"pass%sword\" />";
        for(String specialCharacter : specialCharacters)
        {
            withXmlElement(annotatedObject, format(input, specialCharacter));
            String sanitized = getSourceXML(annotatedObject);
            assertThat(sanitized, equalTo("<sftp:config username=\"user\" password=\"<<credentials>>\" />"));
        }
    }

    @Test
    public void dwNonLiteralPasswordMasked()
    {
        String xmlDWGeneratedPassword =
                "<http:body ><![CDATA[#[\"grant_type=password&client_id=mt2-web-ui&username=\" ++ dw::core::URL::encodeURIComponent(vars.keycloakUsername!) ++ \"&password=\" ++ dw::core::URL::encodeURIComponent(vars.keycloakPassword!)]]]>\\"
                        +
                        "</http:body>\n<http:headers><![CDATA[#[output application/java --- {\"Accept\" : \"application/json\",\"Content-Type\" : \"application/x-www-form-urlencoded\"]]>\\"
                        +
                        "</http:headers>";
        withXmlElement(annotatedObject, xmlDWGeneratedPassword);
        String sanitized = getSourceXML(annotatedObject);
        assertThat(sanitized, equalTo(xmlDWGeneratedPassword));
    }


    private void withXmlElement(AnnotatedObject annotatedObject, String value)
    {
        when(annotatedObject.getAnnotation(SOURCE_ELEMENT_ANNOTATION_KEY)).thenReturn(value);
    }
}
