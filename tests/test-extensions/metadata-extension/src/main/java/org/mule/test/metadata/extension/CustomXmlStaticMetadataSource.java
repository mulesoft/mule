/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.metadata.extension;

import static org.mule.runtime.extension.api.annotation.param.MediaType.APPLICATION_XML;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.metadata.fixed.AttributesXmlType;
import org.mule.runtime.extension.api.annotation.metadata.fixed.OutputXmlType;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;

import java.io.InputStream;

@Alias("xml-static-metadata")
@AttributesXmlType(schema = "order.xsd", qname = "shiporder")
@OutputXmlType(schema = "order.xsd", qname = "shiporder")
@MediaType(value = APPLICATION_XML, strict = false)
public class CustomXmlStaticMetadataSource extends Source<InputStream, InputStream> {

  @Override
  public void onStart(SourceCallback sourceCallback) throws MuleException {

  }

  @Override
  public void onStop() {

  }
}
