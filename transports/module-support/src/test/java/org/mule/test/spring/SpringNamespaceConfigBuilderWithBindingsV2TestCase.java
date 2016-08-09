/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.spring;

import org.mule.functional.AbstractConfigBuilderWithBindingsTestCase;

/**
 * This is an extended version of the same test covered in {@link SpringNamespaceConfigBuilderWithBindingsTestCase}. Both are
 * translations of an earlier (1.X) test.
 * <p/>
 * I realise this seems rather messy, and I did consider merging the two, but they often test different things, and we would have
 * lost quite a few tests on merging. So I am afraid we are left with two rather rambling, parallel tests. But these tests examing
 * "corner cases" no other tests cover, so are quite valuable...
 */
public class SpringNamespaceConfigBuilderWithBindingsV2TestCase extends AbstractConfigBuilderWithBindingsTestCase {

  public SpringNamespaceConfigBuilderWithBindingsV2TestCase() {
    super(true);
    setDisposeContextPerClass(true);
  }

  @Override
  public String[] getConfigFiles() {
    return new String[] {"org/mule/test/spring/config2/test-xml-mule2-config.xml",
        "org/mule/test/spring/config2/test-xml-mule2-config-split.xml"};
  }
}
