/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.scripting.component;

import static org.mule.runtime.module.scripting.component.Scriptable.BINDING_MESSAGE;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.LifecycleUtils;
import org.mule.runtime.core.component.AbstractComponent;

import javax.script.Bindings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Script service backed by a JSR-223 compliant script engine such as Groovy, JavaScript, or Rhino.
 */
public class ScriptComponent extends AbstractComponent {

  private static final Logger LOGGER = LoggerFactory.getLogger(ScriptComponent.class);

  private Scriptable script;

  @Override
  protected void doInitialise() throws InitialisationException {
    LifecycleUtils.initialiseIfNeeded(script, muleContext);
    super.doInitialise();
  }

  @Override
  protected void doDispose() {
    LifecycleUtils.disposeIfNeeded(script, LOGGER);
  }

  @Override
  protected Object doInvoke(MuleEvent event) throws Exception {
    // Set up initial script variables.
    Bindings bindings = script.getScriptEngine().createBindings();
    putBindings(bindings);
    script.populateBindings(bindings, event);
    try {
      return script.runScript(bindings);
    } catch (Exception e) {
      // leave this catch block in place to help debug classloading issues
      throw e;
    } finally {
      event.setMessage((MuleMessage) bindings.get(BINDING_MESSAGE));
      bindings.clear();
    }
  }

  protected void putBindings(Bindings bindings) {
    // template method
  }

  public Scriptable getScript() {
    return script;
  }

  public void setScript(Scriptable script) {
    this.script = script;
  }
}
