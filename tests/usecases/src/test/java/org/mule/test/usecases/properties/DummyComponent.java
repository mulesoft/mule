package org.mule.test.usecases.properties;

import org.mule.impl.RequestContext;

import java.util.ArrayList;
import java.util.List;

public class DummyComponent {

     public void processData(String theData) {
         System.out.println(theData);
         List recipients = new ArrayList();
         recipients.add("ross.mason@cubis.co.uk");
         recipients.add("ross@rossmason.com");
         RequestContext.setProperty("recipients", recipients);
     }
}
