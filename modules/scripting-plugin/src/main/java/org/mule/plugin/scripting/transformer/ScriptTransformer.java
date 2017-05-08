/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.plugin.scripting.transformer;

import org.mule.plugin.scripting.component.Scriptable;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.LifecycleUtils;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.transformer.AbstractMessageTransformer;

import java.nio.charset.Charset;

import javax.script.Bindings;
import javax.script.ScriptException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs a script to perform transformation on an object.
 */
public class ScriptTransformer extends AbstractMessageTransformer {

  private static final Logger LOGGER = LoggerFactory.getLogger(ScriptTransformer.class);

  private Scriptable script;

  @Override
  public void initialise() throws InitialisationException {
    super.initialise();
    LifecycleUtils.initialiseIfNeeded(script, muleContext);
  }

  @Override
  public void dispose() {
    super.dispose();
    LifecycleUtils.disposeIfNeeded(script, LOGGER);
  }

  @Override
  public Object transformMessage(Event event, Charset outputEncoding) throws TransformerException {
    Bindings bindings = script.getScriptEngine().createBindings();
    script.populateBindings(bindings, event, Event.builder(event));
    try {
      return script.runScript(bindings);
    } catch (ScriptException e) {
      throw new TransformerException(this, e);
    } finally {
      bindings.clear();
    }
  }

  public Scriptable getScript() {
    return script;
  }

  public void setScript(Scriptable script) {
    this.script = script;
  }
}
