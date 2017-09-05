package org.mule.functional.api.component;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.InternalEvent;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.exception.AbstractExceptionListener;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.exception.MessagingExceptionHandlerAcceptor;

public class OnErrorAssertHandler extends AbstractExceptionListener implements MessagingExceptionHandlerAcceptor{

  private String expectedLogMessage;

  @Override
  protected void doInitialise(MuleContext muleContext) throws InitialisationException {
    super.doInitialise(muleContext);
  }

  @Override
  public InternalEvent handleException(MessagingException exception, InternalEvent event) {
    String messageToLog = createMessageToLog(exception);
    exception.setHandled(true);
    return null;
  }

  @Override
  public boolean accept(InternalEvent event) {
    return true;
  }

  @Override
  public boolean acceptsAll() {
    return true;
  }

  public void setExpectedLogMessage(String expectedLogMessage) {
    this.expectedLogMessage = expectedLogMessage;
  }

  public String getExpectedLogMessage() {
    return this.expectedLogMessage;
  }

}
