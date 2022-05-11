/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el.mvel;

import org.mule.mvel2.ParserConfiguration;
import org.mule.mvel2.integration.VariableResolver;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;

public class VariableVariableResolverFactory extends MuleBaseVariableResolverFactory {

  private static final long serialVersionUID = -4433478558175131280L;

  private PrivilegedEvent event;
  private PrivilegedEvent.Builder eventBuilder;

  // TODO MULE-10471 Immutable event used in MEL/Scripting should be shared for consistency
  public VariableVariableResolverFactory(ParserConfiguration parserConfiguration, MuleContext muleContext, PrivilegedEvent event,
                                         PrivilegedEvent.Builder eventBuilder) {
    this.event = event;
    this.eventBuilder = eventBuilder;
  }

  @SuppressWarnings("deprecation")
  @Override
  public boolean isTarget(String name) {
    if (event == null) {
      return false;
    }
    return event.getVariables().containsKey(name)
        || (event.getSession() != null && event.getSession().getPropertyNamesAsSet().contains(name));
  }

  @SuppressWarnings("deprecation")
  @Override
  public VariableResolver getVariableResolver(String name) {

    if (event != null && event.getVariables().containsKey(name)) {
      return new FlowVariableVariableResolver(name);
    } else if (event != null && event.getSession().getPropertyNamesAsSet().contains(name)) {
      return new SessionVariableVariableResolver(name);
    } else {
      return super.getNextFactoryVariableResolver(name);
    }
  }

  @SuppressWarnings("rawtypes")
  class FlowVariableVariableResolver implements VariableResolver {

    private static final long serialVersionUID = -4847663330454657440L;

    String name;

    public FlowVariableVariableResolver(String name) {
      this.name = name;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public Class getType() {
      return Object.class;
    }

    @Override
    public void setStaticType(Class type) {}

    @Override
    public int getFlags() {
      return 0;
    }

    @Override
    public Object getValue() {
      return event.getVariables().get(name).getValue();
    }

    @Override
    public void setValue(Object value) {
      eventBuilder.addVariable(name, value);
      event = eventBuilder.build();
    }
  }

  @SuppressWarnings({"deprecation", "rawtypes"})
  class SessionVariableVariableResolver implements VariableResolver {

    private static final long serialVersionUID = 7658449705305592397L;

    private String name;

    public SessionVariableVariableResolver(String name) {
      this.name = name;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public Class getType() {
      return Object.class;
    }

    @Override
    public void setStaticType(Class type) {}

    @Override
    public int getFlags() {
      return 0;
    }

    @Override
    public Object getValue() {
      return event.getSession().getProperty(name);
    }

    @Override
    public void setValue(Object value) {
      event.getSession().setProperty(name, value);
    }
  }

}
