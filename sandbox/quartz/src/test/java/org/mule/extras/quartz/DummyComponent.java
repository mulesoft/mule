package org.mule.extras.quartz;

import EDU.oswego.cs.dl.util.concurrent.WaitableInt;

public class DummyComponent {

	public static WaitableInt countCalled = new WaitableInt(0); 
	
	public DummyComponent() {
	}
	
	public void handle(DummyPayload p) {
		System.err.println("Received: " + p.toString());
		countCalled.increment();
	}

}
