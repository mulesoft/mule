package org.jbpm.msg.mule;

import org.jbpm.svc.Service;
import org.jbpm.svc.ServiceFactory;

public class MuleMessageServiceFactory implements ServiceFactory {

  private static final long serialVersionUID = 1L;

  public Service openService() {
    return new MuleMessageService();
  }

  public void close() {
  }

}
