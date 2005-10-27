Quick guide to getting Mule IDE to work:
- Import the four projects into Eclipse
- Launch an "Eclipse Application" using these four plugins.
- Go to the Preferences and set the Mule root.
- Create a Java project into which you import any Java classes you want to put in your Mule.(you will have to manually attach the mule-core JAR file, this can be fixed in the net version.
- Now click Debug... choose Mule Config, click New, click on the "..." button where it says Projects, and pick the project you just created/used.
- Now you will be asked if you want to add the Mule UMO nature to your project (this is also controllable in the project Preferences page)
- Choose the config file, which must also reside in your project.
- Debug it! The console can be used for input and output (if. e.g. running the sample).

For kicks, introduce an error in a Mule config file in the project and save it...