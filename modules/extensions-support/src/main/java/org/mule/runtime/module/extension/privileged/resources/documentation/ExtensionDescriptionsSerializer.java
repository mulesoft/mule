/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.privileged.resources.documentation;

import static org.mule.runtime.module.extension.internal.ExtensionProperties.EXTENSION_DESCRIPTIONS_FILE_NAME_MASK;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;

import org.mule.apache.xml.serialize.OutputFormat;
import org.mule.apache.xml.serialize.XMLSerializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

/**
 * A simple XML JAXB serializer class for {@link XmlExtensionDocumentation}s files.
 *
 * @since 4.0
 */
public class ExtensionDescriptionsSerializer {

  public static final ExtensionDescriptionsSerializer SERIALIZER = new ExtensionDescriptionsSerializer();

  private JAXBContext jaxbContext;
  private Marshaller marshaller;
  private Unmarshaller unmarshaller;

  private ExtensionDescriptionsSerializer() {
    final ClassLoader tccl = currentThread().getContextClassLoader();
    currentThread().setContextClassLoader(ExtensionDescriptionsSerializer.class.getClassLoader());
    try {
      jaxbContext = JAXBContext.newInstance(XmlExtensionDocumentation.class);
      marshaller = jaxbContext.createMarshaller();
      unmarshaller = jaxbContext.createUnmarshaller();
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      currentThread().setContextClassLoader(tccl);
    }
  }

  public synchronized String serialize(XmlExtensionDocumentation dto) {
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
      marshaller.marshal(dto, getXmlSerializer(out).asContentHandler());

      return out.toString();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public synchronized XmlExtensionDocumentation deserialize(String xml) {
    try {
      return (XmlExtensionDocumentation) unmarshaller.unmarshal(new ByteArrayInputStream(xml.getBytes()));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public synchronized XmlExtensionDocumentation deserialize(InputStream xml) {
    try {
      return (XmlExtensionDocumentation) unmarshaller.unmarshal(xml);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private XMLSerializer getXmlSerializer(OutputStream out) {
    OutputFormat of = new OutputFormat();

    of.setCDataElements(new String[] {"^description"});
    of.setIndenting(true);

    XMLSerializer serializer = new XMLSerializer(of);
    serializer.setOutputByteStream(out);

    return serializer;
  }

  public String getFileName(String extensionName) {
    String key = extensionName.replace(" ", "-").toLowerCase();
    return format(EXTENSION_DESCRIPTIONS_FILE_NAME_MASK, key);
  }
}
