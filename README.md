CodeScan IntelliJ Plugin
=========================

[![Build Status](https://dev.azure.com/sonarsource/DotNetTeam%20Project/_apis/build/status/sonarlint/CodeScan%20IntelliJ?branchName=master)](https://dev.azure.com/sonarsource/DotNetTeam%20Project/_build/latest?definitionId=76&branchName=master) [![Quality Gate](https://next.sonarqube.com/sonarqube/api/project_badges/measure?project=org.sonarsource.sonarlint.intellij%3Acodescan-intellij&metric=alert_status
)](https://next.sonarqube.com/sonarqube/dashboard?id=org.sonarsource.sonarlint.intellij%3Acodescan-intellij)

CodeScan is an IDE extension that helps you detect and fix quality issues as you write code.
Like a spell checker, CodeScan squiggles flaws so they can be fixed before committing code.

Useful links
------------

- [CodeScan website](https://www.codescan.io)


How to install
--------------

You can install CodeScan from the [JetBrains Plugin Repository](https://plugins.jetbrains.com/plugin/16087-codescan), directly available in the IDE preferences.

Node.js >= 8.x is required to perform JavaScript or TypeScript analysis.

Have Question or Feedback?
--------------------------

For CodeScan support questions ("How do I?", "I got this error, why?", ...), please first read the [FAQ](https://community.sonarsource.com/t/frequently-asked-questions/7204) and then head to the [SonarSource forum](https://community.sonarsource.com/c/help/sl). There are chances that a question similar to yours has already been answered. 

Be aware that this forum is a community, so the standard pleasantries ("Hi", "Thanks", ...) are expected. And if you don't get an answer to your thread, you should sit on your hands for at least three days before bumping it. Operators are not standing by. :-)

Contributing
------------

If you would like to see a new feature, please create a new thread in the forum ["Suggest new features"](https://community.sonarsource.com/c/suggestions/features).

Please be aware that we are not actively looking for feature contributions. The truth is that it's extremely difficult for someone outside SonarSource to comply with our roadmap and expectations. Therefore, we typically only accept minor cosmetic changes and typo fixes.

With that in mind, if you would like to submit a code contribution, please create a pull request for this repository. Please explain your motives to contribute this change: what problem you are trying to fix, what improvement you are trying to make.

Make sure that you follow our [code style](https://github.com/SonarSource/sonar-developer-toolset#code-style-configuration-for-intellij) and all tests are passing.

How to build
------------

    ./gradlew buildPlugin

Note that the above won't run tests and checks. To do that too, run:

    ./gradlew check buildPlugin

For the complete list of tasks, see:

    ./gradlew tasks

How to run ITs
------------

    ./gradlew :its:check

The above will start an IDE instance, wait for the UI robot server to start, run the ITs and finally close the IDE.

To test against a specific version of IntelliJ, the `ijVersion` property can be used, e.g.:

    ./gradlew :its:check -PijVersion=IC-2019.3

In development mode, it can be handy to separately start the IDE and run the tests, as follows:

    ./gradlew :its:runIdeForUiTests
    ./gradlew :its:test

The `:its:runIdeForUiTests` task is blocking. Also please note that the IDE must be in foreground while tests are executed.

How to develop in IntelliJ
--------------------------

Import the project as a Gradle project.

Note: whenever you change a Gradle setting (for example in `build.gradle`),
don't forget to **Refresh all Gradle projects** in the **Gradle** toolbar.

To run an IntelliJ instance with the plugin installed, execute the Gradle task `runIde` using the command line,
or the **Gradle** toolbar in IntelliJ, under `Tasks/intellij`.
The instance files are stored under `build/idea-sandbox`.

Keep in mind that the `clean` task will wipe out the content of `build/idea-sandbox`,
so you will need to repeat some setup steps for that instance, such as configuring the JDK.

Whenever you change dependency version, the previous versions are not deleted from the sandbox, and the JVM might not load the version that you expect.
As the `clean` task may be inconvenient, an easier workaround is to delete the jars in the sandbox, for example with:

    find build/idea-sandbox/ -name '*.jar' -delete

How to release
--------------

See [release pipeline at Azure DevOps](https://dev.azure.com/sonarsource/DotNetTeam%20Project/_release?definitionId=10).

License
-------

Copyright 2015-2021 SonarSource.

Licensed under the [GNU Lesser General Public License, Version 3.0](http://www.gnu.org/licenses/lgpl.txt)
