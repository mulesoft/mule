/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.scripting.filter;

import static org.mule.runtime.core.DefaultMessageContext.create;
import static org.mule.runtime.core.MessageExchangePattern.ONE_WAY;
import static org.mule.runtime.module.scripting.component.Scriptable.BINDING_MESSAGE;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.lifecycle.Disposable;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.LifecycleUtils;
import org.mule.runtime.core.api.routing.filter.Filter;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.processor.AbstractFilteringMessageProcessor;
import org.mule.runtime.module.scripting.component.Scriptable;

import javax.script.Bindings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScriptFilter extends AbstractFilteringMessageProcessor implements Filter, Initialisable, Disposable {

  private static final Logger LOGGER = LoggerFactory.getLogger(ScriptFilter.class);

  private Scriptable script;

  private String name;

  @Override
  public void initialise() throws InitialisationException {
    LifecycleUtils.initialiseIfNeeded(script, muleContext);
  }

  @Override
  public void dispose() {
    LifecycleUtils.disposeIfNeeded(script, LOGGER);
  }

  @Override
  public boolean accept(MuleEvent event) {
    Bindings bindings = script.getScriptEngine().createBindings();

    script.populateBindings(bindings, event);
    try {
      return (Boolean) script.runScript(bindings);
    } catch (Throwable e) {
      // TODO MULE-9356 ScriptFilter should rethrow exceptions, or at least log, not ignore them
      return false;
    } finally {
      event.setMessage((MuleMessage) bindings.get(BINDING_MESSAGE));
    }
  }

  @Override
  public boolean accept(MuleMessage message) {
    Bindings bindings = script.getScriptEngine().createBindings();

    // TODO MULE-9341 Remove Filters.
    Flow flow = new Flow("", muleContext);
    MuleEvent event =
        MuleEvent.builder(create(flow, "ScriptFilter")).message(message).exchangePattern(ONE_WAY).flow(flow).build();
    script.populateBindings(bindings, event);
    try {
      return (Boolean) script.runScript(bindings);
    } catch (Throwable e) {
      // TODO MULE-9356 ScriptFilter should rethrow exceptions, or at least log, not ignore them
      return false;
    }
  }

  public Scriptable getScript() {
    return script;
  }

  public void setScript(Scriptable script) {
    this.script = script;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}


