package org.mule.functional.api.component;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.InternalEvent;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.exception.AbstractExceptionListener;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.exception.MessagingExceptionHandlerAcceptor;

import java.util.List;

public class OnErrorAssertHandler extends AbstractExceptionListener implements MessagingExceptionHandlerAcceptor{


  private List<LogChecker> checkers;

  @Override
  protected void doInitialise(MuleContext muleContext) throws InitialisationException {
    super.doInitialise(muleContext);
  }

  @Override
  public InternalEvent handleException(MessagingException exception, InternalEvent event) {
    String messageToLog = createMessageToLog(exception);
    for( LogChecker checker : this.checkers) {
      checker.check(messageToLog);
    }
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

  public void setCheckers(List<LogChecker> logCheckers) {
    this.checkers = logCheckers;
  }

  public List<LogChecker> getCheckers() {
    return this.checkers;
  }

}
