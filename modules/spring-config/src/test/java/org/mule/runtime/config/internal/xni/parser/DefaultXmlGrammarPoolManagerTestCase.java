/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.xni.parser;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.mule.runtime.config.internal.xni.parser.DefaultXmlGrammarPoolManager.getGrammarPool;

import com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarPool;
import org.junit.Test;

public class DefaultXmlGrammarPoolManagerTestCase {

  @Test
  public void grammarPoolManagerShouldReturnSingletonInstance() {
    XMLGrammarPool grammarPool = getGrammarPool();
    assertThat(grammarPool, is(notNullValue()));
    assertThat(grammarPool, is(instanceOf(RuntimeXmlGrammarPool.class)));
    XMLGrammarPool grammarPool2 = getGrammarPool();
    assertThat(grammarPool, is(sameInstance(grammarPool2)));
  }
}
