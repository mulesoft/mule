/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.api.component;

import org.mule.runtime.core.api.component.JavaComponent;
import org.mule.runtime.core.api.model.EntryPointResolverSet;

import java.util.List;

/**
 * A <code>JavaComponent</code> uses a customizable {@link EntryPointResolverSet} in order to resolve which method should be used
 * for invocation and allows java bindings to be configure. Java Component bindings, if implemented by the JavaComponent
 * implementation, uses a component instance proxy to implement interface methods using calls to outbound endpoints.
 * 
 * @deprecated Transport infrastructure is deprecated.
 */
@Deprecated
public interface JavaWithBindingsComponent extends JavaComponent {

  List<InterfaceBinding> getInterfaceBindings();

  void setInterfaceBindings(List<InterfaceBinding> bindgins);
}
