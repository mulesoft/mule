/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import static java.util.Collections.emptySet;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.isEmptyString;
import static org.junit.Assert.assertThat;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.tck.size.SmallTest;

import org.junit.Test;
import org.xml.sax.InputSource;

@SmallTest
public class ModuleDelegatingEntityResolverTestCase {

  private ModuleDelegatingEntityResolver resolver;

  @Test
  public void legacySpring() throws Exception {
    resolver = new ModuleDelegatingEntityResolver(emptySet());

    InputSource source = resolver.resolveEntity(null, "http://www.springframework.org/schema/beans/spring-beans-current.xsd");
    assertThat(source, is(not(nullValue())));
    assertThat(IOUtils.toString(source.getByteStream()), is(not(isEmptyString())));
  }
}
