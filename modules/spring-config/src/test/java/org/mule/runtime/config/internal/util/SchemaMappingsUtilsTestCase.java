/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.runtime.config.AllureConstants.DslParsing.DSL_PARSING;
import static org.mule.runtime.config.AllureConstants.DslParsing.XmlGrammarPool.XML_GRAMMAR_POOL;
import static org.mule.runtime.config.internal.util.SchemaMappingsUtils.resolveSystemId;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(DSL_PARSING)
@Story(XML_GRAMMAR_POOL)
public class SchemaMappingsUtilsTestCase {

  private static final String LEGACY_SPRING_XSD = "http://www.springframework.org/schema/beans/spring-beans-current.xsd";
  private static final String CORE_XSD = "http://www.mulesoft.org/schema/mule/core/current/mule.xsd";
  private static final String CORE_CURRENT_XSD = "http://www.mulesoft.org/schema/mule/core/current/mule-core.xsd";
  private static final String CORE_DEPRECATED_XSD = "http://www.mulesoft.org/schema/mule/core/current/mule-core-deprecated.xsd";
  private static final String COMPATIBILITY_XSD =
      "http://www.mulesoft.org/schema/mule/compatibility/current/mule-compatibility.xsd";
  private static final String UNKNOW_XSD = "http://www.mulesoft.org/schema/mule/core/current/mule-unknow.xsd";

  @Test
  @Issue("MULE-16572")
  public void legacySpring() {
    String systemId = resolveSystemId(null, LEGACY_SPRING_XSD, (pId, sId) -> false);
    assertThat(systemId, is("http://www.springframework.org/schema/beans/spring-beans.xsd"));
  }

  @Test
  public void unknownSystemIdShouldNotBeResolved() {
    String systemId = resolveSystemId(null, UNKNOW_XSD, (pId, sId) -> false);
    assertThat(systemId, is(systemId));
  }

  @Test
  public void deprecatedCoreXsdShouldBeResolvedIfIsRunningTestsAndDeprecatedCanBeResolved() {
    String systemId = resolveSystemId(null, CORE_XSD, true, (pId, sId) -> sId.equals(CORE_DEPRECATED_XSD));
    assertThat(systemId, is(CORE_DEPRECATED_XSD));

    systemId = resolveSystemId(null, CORE_XSD, (pId, sId) -> sId.equals(CORE_DEPRECATED_XSD));
    assertThat(systemId, is(CORE_CURRENT_XSD));
  }

  @Test
  public void deprecatedCoreXsdShouldBeResolvedIfCanResolveCompatibilityAndDeprecatedXsd() {
    String systemId =
        resolveSystemId(null, CORE_XSD, (pId, sId) -> sId.equals(COMPATIBILITY_XSD) || sId.equals(CORE_DEPRECATED_XSD));
    assertThat(systemId, is(CORE_DEPRECATED_XSD));
  }

  @Test
  public void coreCurrentXsdShouldBeResolvedIfCoreXsdCanNotBeResolvedAsDeprecatedOrCompatibility() {
    String systemId = resolveSystemId(null, CORE_XSD, (pId, sId) -> false);
    assertThat(systemId, is(CORE_CURRENT_XSD));
  }
}
