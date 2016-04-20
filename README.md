Stop the point-to-point madness
===============================
Mule is a lightweight integration platform that allows you to connect anything anywhere. Rather than creating point-to-point integrations between systems, services, APIs and devices, you can use Mule to intelligently manage message-routing, data mapping, orchestration, reliability, security and scalability between nodes. Plug other systems and applications into Mule and let it handle all the communication betweens systems, enabling you track and monitor everything that happens. 

At the simplest level, Mule applications accept and process messages through several Lego-block-like message processors plugged together in what we call a flow. Understanding the basic flow architecture is key to understanding Mule. Essentially every Mule flow contains a series of building blocks that accept, then transform and process messages. 

Receive
=======
Based on the concept of Event Driven Architecture (EDA), Mule works by responding to messages initiated by external resources (i.e. events). For example, a message can be initiated by an event such as a consumer request from a mobile device, or a change to data in a database, or the creation of a new customer ID in a SaaS application. 
 In every flow, there must be a receiver to accept new messages for processing. Mule uses a message source element to receive messages from one or more external sources, thus triggering the execution of a flow. A transport carries the message along as it passes through the integration and application levels for processing.

Transform
=========
Mule transformers are the key to exchanging data between nodes, as they allow Mule to convert message payload data to a format that another application can understand. Mule also enables content enrichment of messages which allows you to retrieve additional data during processing and attach it to the message.

Process
=======
Mule uses components to conduct backend processes for specific business logic (like checking the customer and inventory databases). Then, the components route messages to the correct application (such as an order fulfillment system). Importantly, components don't have to have any Mule-specific code; they can simply be POJOs, Spring beans, Java beans, Groovy scripts, or web services containing the business logic for processing data. Components can even be developed in other languages such as Python, JavaScript, Ruby, and PHP. Mule’s catalog of building blocks includes the most commonly used Enterprise Integration Patterns.

A flow, therefore, is the construct within which you link together several individual components (i.e. building blocks) to handle the receipt, processing, and eventual routing of a message. You can connect many flows together to build a complete application which you can then deploy on premise, on Mule, on another application server, or in the cloud. Practically speaking, you could build, deploy, and run a Mule application in a matter of hours, rather than spending weeks or months building point-to-point connections between systems. Studio, Mule’s Eclipse-based graphical IDE, makes it even easier to model, then configure, test, and deploy your applications. Mule is so named because it “carries the heavy development load” of connecting systems.

Get Started
===========

1. Download, then unzip Mule – either the Standalone version (Mule runtime engine) or Anypoint Studio (the runtime engine + the Eclipse-based graphical IDE) – from [mulesoft.com](https://www.mulesoft.com).

1. Confirm that you have a [JDK](http://www.oracle.com/technetwork/java/javase/downloads/index.html) on your local drive, Java SE 7 Update 80 or SE 8 (Update 45).

1. Launch Mule Studio by double-clicking AnypointStudio.app (Mac) or AnypointStudio.exe (Windows).  
     Launch Mule as a standalone from the command line by executing ./bin/mule (Mac) or mule.bat (Windows). 
     Refer to the [Mule Installation Guide](https://developer.mulesoft.com/docs/display/current/Installing) for more details. 

Mule is up and kicking! Check out [Anypoint Exchange](https://www.mulesoft.com/exchange) to explore the potential of Mule.


Contribute
==========
Mule is open source and we love contributions! If you have an idea for a great improvement or spy an issue you’re keen to fix, you can fork us on [github](https://github.com/mulesoft/mule).

No contribution is too small – providing feedback, [reporting issues](http://www.mulesoft.org/jira/browse/MULE) and participating in the [community forums](http://forum.mulesoft.org/mulesoft) is invaluable and extremely helpful for all our users. Please refer to our [contribution guidelines](CONTRIBUTING.md) for details.




