/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.dsl;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.test.allure.AllureConstants.MuleDsl.DslParsingStory.DSL_PARSING_STORY;
import static org.mule.test.allure.AllureConstants.MuleDsl.MULE_DSL;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.meta.AnnotatedObject;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.Optional;

import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features(MULE_DSL)
@Stories(DSL_PARSING_STORY)
public class CustomizedXmlNamespacePrefixTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/dsl/customized-namespace-prefix-config.xml";
  }

  @Test
  public void validateThatACustomXmlNamespacePrefixCanBeUsed() {
    Optional<AnnotatedObject> httpRequesterOptional = muleContext.getConfigurationComponentLocator()
        .find(Location.builder().globalName("flow").addProcessorsPart().addIndexPart(0).build());
    assertThat(httpRequesterOptional.isPresent(), is(true));
    assertThat(httpRequesterOptional.get().getLocation().getComponentIdentifier().getIdentifier(),
               is(buildFromStringRepresentation("httpn:request")));
  }
}
