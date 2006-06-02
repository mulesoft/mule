package org.mule.test.usecases.properties;

import org.mule.impl.RequestContext;

import java.util.ArrayList;
import java.util.List;

public class DummyComponent {

     public void processData(String theData) {
         System.out.println(theData);
         List recipients = new ArrayList();
         recipients.add("ross.mason@symphonysoft.com");
         recipients.add("ross@rossmason.com");
         RequestContext.getEventContext().getMessage().setProperty("recipients", recipients);
     }
}
