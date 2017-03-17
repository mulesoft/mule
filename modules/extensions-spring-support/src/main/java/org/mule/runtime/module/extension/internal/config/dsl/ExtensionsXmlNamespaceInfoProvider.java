/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl;

import static java.util.Arrays.asList;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.dsl.api.xml.XmlNamespaceInfo;
import org.mule.runtime.dsl.api.xml.XmlNamespaceInfoProvider;

import java.util.Collection;

import javax.inject.Inject;


/**
 * A {@link XmlNamespaceInfoProvider} which provides the information for the {@link ExtensionXmlNamespaceInfo#EXTENSION_NAMESPACE}
 * namespace.
 *
 * @since 4.0
 */
public class ExtensionsXmlNamespaceInfoProvider implements XmlNamespaceInfoProvider {

  @Inject
  private MuleContext context;

  @Override
  public Collection<XmlNamespaceInfo> getXmlNamespacesInfo() {
    return asList(new ExtensionXmlNamespaceInfo());
  }
}
