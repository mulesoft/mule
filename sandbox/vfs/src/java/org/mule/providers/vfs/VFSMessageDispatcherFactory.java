package org.mule.providers.vfs;

import org.mule.umo.UMOException;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.provider.UMOMessageDispatcher;
import org.mule.umo.provider.UMOMessageDispatcherFactory;

/**
 *
 * User: Ian de Beer
 * Date: May 29, 2005
 * Time: 1:35:55 PM
 */
public class VFSMessageDispatcherFactory implements UMOMessageDispatcherFactory {
  public UMOMessageDispatcher create(UMOConnector connector) throws UMOException {
    return new org.mule.providers.vfs.VFSMessageDispatcher((org.mule.providers.vfs.VFSConnector) connector);
  }
}
