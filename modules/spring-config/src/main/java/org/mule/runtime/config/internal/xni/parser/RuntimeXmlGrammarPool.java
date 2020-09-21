/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.xni.parser;

import com.sun.org.apache.xerces.internal.xni.grammars.Grammar;
import com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarDescription;
import com.sun.org.apache.xerces.internal.xni.grammars.XMLGrammarPool;
import org.mule.runtime.api.util.LazyValue;

/**
 * A read-only {@link XMLGrammarPool} preloaded with mule schemas
 *
 * @since 4.4.0
 */
public class RuntimeXmlGrammarPool implements XMLGrammarPool {

  private final LazyValue<XMLGrammarPool> core;

  public RuntimeXmlGrammarPool(LazyValue<XMLGrammarPool> core) {
    this.core = core;
  }

  @Override
  public Grammar[] retrieveInitialGrammarSet(String s) {
    return core.get().retrieveInitialGrammarSet(s);
  }

  @Override
  public Grammar retrieveGrammar(XMLGrammarDescription xmlGrammarDescription) {
    return core.get().retrieveGrammar(xmlGrammarDescription);
  }

  @Override
  public void cacheGrammars(String s, Grammar[] grammars) {
    // Nothing to do
  }

  @Override
  public void lockPool() {
    // Nothing to do
  }

  @Override
  public void unlockPool() {
    // Nothing to do
  }

  @Override
  public void clear() {
    // Nothing to do
  }
}
