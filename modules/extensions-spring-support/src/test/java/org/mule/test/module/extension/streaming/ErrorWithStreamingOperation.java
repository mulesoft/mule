package org.mule.test.module.extension.streaming;

import static org.hamcrest.Matchers.containsString;

import org.mule.functional.api.exception.ExpectedError;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import org.junit.Rule;
import org.junit.Test;

public class ErrorWithStreamingOperation extends AbstractExtensionFunctionalTestCase {

  private final long AMOUNT_OF_CALLS = 10;
  private final String SIGNATURE = "This is my signature";

  @Rule
  public ExpectedError expectedError = ExpectedError.none();

  @Override
  protected String getConfigFile() {
    return "error-with-streaming-operation-config.xml";
  }

  @Test
  public void connectionIsReleasedWhenOperationFails() throws Exception{
    expectedError.expectErrorType("MULE", "UNKNOWN");
    expectedError.expectMessage(containsString("The operation failed!"));

    for(int i=0; i<AMOUNT_OF_CALLS; i++){
      try{
        flowRunner("streaming-operation-with-error").withVariable("signature", SIGNATURE).run();
      } catch (Exception e) {
        // Do Nothing
      }
    }

    flowRunner("streaming-operation-with-error").withVariable("signature", SIGNATURE).run();
  }

}
