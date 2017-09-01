package org.mule.functional.api.component;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.InternalEvent;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.internal.exception.TemplateOnErrorHandler;

public class OnErrorAssertHandler extends TemplateOnErrorHandler{


  public OnErrorAssertHandler() {
    setHandleException(true);
  }

  @Override
  protected void doInitialise(MuleContext muleContext) throws InitialisationException {
    super.doInitialise(muleContext);
  }

  @Override
  public boolean acceptsAll() {
    return true;
  }

  @Override
  public boolean accept(InternalEvent event) {
    return super.accept(event);
  }

  @Override
  protected void doLogException(Throwable t) {
    logger.error("\n\nTHIS IS AN ERROR\n\n");
  }

}
