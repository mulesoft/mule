/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.transport;

import java.util.Iterator;

import javax.xml.transform.Source;

import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.binding.soap.SoapVersionFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.databinding.stax.StaxDataBinding;
import org.apache.cxf.databinding.stax.StaxDataBindingFeature;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientFactoryBean;
import org.apache.cxf.transport.AbstractTransportFactory;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.ConduitInitiatorManager;

/**
 * A factory for CXF {@link Client}s.
 *
 * Sets up the custom {@link WscConduitInitiator} for all the different entries used for CXF to obtain the needed {@link Conduit},
 * this occurs because we want that CXF always use the {@link WscConduit} to operate.
 *
 * @since 4.0
 */
public class WscTransportFactory extends AbstractTransportFactory {

  public WscTransportFactory() {
    if (bus == null) {
      bus = new SpringBusFactory().createBus((String) null, true);
    }
    WscConduitInitiator initiator = new WscConduitInitiator();
    ConduitInitiatorManager extension = bus.getExtension(ConduitInitiatorManager.class);

    extension.registerConduitInitiator("http://cxf.apache.org/transports/http", initiator);
    extension.registerConduitInitiator("http://schemas.xmlsoap.org/wsdl/soap/", initiator);
    extension.registerConduitInitiator("http://schemas.xmlsoap.org/soap/http/", initiator);
    extension.registerConduitInitiator("http://schemas.xmlsoap.org/wsdl/soap/http", initiator);
    extension.registerConduitInitiator("http://schemas.xmlsoap.org/wsdl/http/", initiator);
    extension.registerConduitInitiator("http://www.w3.org/2003/05/soap/bindings/HTTP/", initiator);
    extension.registerConduitInitiator("http://cxf.apache.org/transports/http/configuration", initiator);
    extension.registerConduitInitiator("http://cxf.apache.org/bindings/xformat", initiator);
    extension.registerConduitInitiator("http://cxf.apache.org/transports/jms", initiator);
    extension.registerConduitInitiator("http://mule.codehaus.org/ws", initiator);
  }

  public Client createClient(String address, String soapVersion) {
    ClientFactoryBean factory = new ClientFactoryBean();
    factory.setServiceClass(ProxyService.class);
    factory.setDataBinding(new StaxDataBinding());
    factory.getFeatures().add(new StaxDataBindingFeature());
    factory.setAddress(address);
    factory.setBus(bus);
    factory.setBindingId(getBindingIdForSoapVersion(soapVersion));

    return factory.create();
  }

  private static String getBindingIdForSoapVersion(String version) {
    Iterator<SoapVersion> soapVersions = SoapVersionFactory.getInstance().getVersions();
    while (soapVersions.hasNext()) {
      SoapVersion soapVersion = soapVersions.next();
      if (Double.toString(soapVersion.getVersion()).equals(version)) {
        return soapVersion.getBindingId();
      }
    }
    throw new IllegalArgumentException("Invalid Soap version " + version);
  }

  /**
   * Interface that describes the implementing the service.
   */
  private interface ProxyService {

    Source invoke(Source source);
  }
}
