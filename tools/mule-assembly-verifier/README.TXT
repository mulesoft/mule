This plugin is written in Groovy. Until some artifacts are officially published by
Codehaus' Mojo project, they are only available from SVN.

Instructions
==================
1. Checkout http://svn.codehaus.org/mojo/trunk/mojo/groovy
2. From the top-level checkout folder execute 'mvn'

Now that all the pre-requisites have been installed, you can use the plugin. It is recommended
to bind its execution to the 'verify' phase, so as to have the final zip produced by the
'package' phase.

The goal name for this plugin is 'verify'.