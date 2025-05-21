/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources.documentation;

import static org.mule.runtime.api.util.xmlsecurity.XMLSecureFactories.createDefault;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.EXTENSION_DESCRIPTIONS_FILE_NAME_MASK;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.nio.charset.StandardCharsets.UTF_8;

import static jakarta.xml.bind.Marshaller.JAXB_FORMATTED_OUTPUT;
import static javax.xml.transform.OutputKeys.CDATA_SECTION_ELEMENTS;
import static javax.xml.transform.OutputKeys.INDENT;

import org.mule.runtime.module.extension.api.resources.documentation.ExtensionDescriptionSerializerException;
import org.mule.runtime.module.extension.api.resources.documentation.ExtensionDescriptionsSerializer;
import org.mule.runtime.module.extension.api.resources.documentation.XmlExtensionDocumentation;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringWriter;

import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

public class DefaultExtensionDescriptionsSerializer implements ExtensionDescriptionsSerializer {

  private final Marshaller marshaller;
  private final Unmarshaller unmarshaller;
  private final Transformer transformer;

  public DefaultExtensionDescriptionsSerializer() {
    final ClassLoader tccl = currentThread().getContextClassLoader();
    try {
      currentThread().setContextClassLoader(DefaultExtensionDescriptionsSerializer.class.getClassLoader());
      JAXBContext jaxbContext = JAXBContext.newInstance(DefaultXmlExtensionDocumentation.class);
      marshaller = jaxbContext.createMarshaller();
      marshaller.setProperty(JAXB_FORMATTED_OUTPUT, true);
      unmarshaller = jaxbContext.createUnmarshaller();
    } catch (Exception e) {
      throw new RuntimeException("Failed to initialize XML serialization components", e);
    } finally {
      currentThread().setContextClassLoader(tccl);
    }

    try {
      transformer = createDefault().getTransformerFactory().newTransformer();
      transformer.setOutputProperty(INDENT, "yes");
      transformer.setOutputProperty(CDATA_SECTION_ELEMENTS, "description");
    } catch (Exception e) {
      throw new ExtensionDescriptionSerializerException("Failed to initialize XML serialization components", e);
    }
  }

  @Override
  public synchronized String serialize(XmlExtensionDocumentation dto) {
    try {
      // First marshal to DOM
      DOMResult domResult = new DOMResult();
      marshaller.marshal(dto, domResult);

      // Then transform DOM to string with proper formatting
      StringWriter writer = new StringWriter();
      transformer.transform(new DOMSource(domResult.getNode()), new StreamResult(writer));
      return writer.toString();
    } catch (Exception e) {
      throw new ExtensionDescriptionSerializerException("Failed to serialize XML documentation", e);
    }
  }

  @Override
  public synchronized XmlExtensionDocumentation deserialize(String xml) {
    return deserialize(new ByteArrayInputStream(xml.getBytes(UTF_8)));
  }

  @Override
  public synchronized XmlExtensionDocumentation deserialize(InputStream xml) {
    try {
      return (DefaultXmlExtensionDocumentation) unmarshaller.unmarshal(xml);
    } catch (Exception e) {
      throw new ExtensionDescriptionSerializerException("Failed to deserialize XML documentation from stream", e);
    }
  }

  @Override
  public String getFileName(String extensionName) {
    String key = extensionName.replace(" ", "-").toLowerCase();
    return format(EXTENSION_DESCRIPTIONS_FILE_NAME_MASK, key);
  }
}

