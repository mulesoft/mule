package org.mule.providers.vfs;

import org.mule.impl.endpoint.AbstractEndpointBuilder;
import org.mule.umo.endpoint.MalformedEndpointException;

import java.net.URI;
import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 * User: Ian de Beer
 * Date: May 29, 2005
 * Time: 1:48:53 AM
 */
public class VFSEndpointBuilder extends AbstractEndpointBuilder {
  protected void setEndpoint(URI uri, Properties props) throws MalformedEndpointException {
    address = uri.getSchemeSpecificPart();
    if (address.startsWith("///")) {
      address = address.substring(2);
    }
    if (address.startsWith("//")) {
      address = address.substring(1);
    }
    int i = address.indexOf("?");
    if (i > -1) {
      address = address.substring(0, i);
    }
  }
}
