# Mule Java Coding Conventions

As a best practice for team development, Mule uses coding conventions to ensure that code is clean and consistent, and that developers are able to maintain efficient productivity.  

## Coding Conventions

Mule follows code formatting conventions configured in the `formatter.xml` file in the project root. This is a configuration that can be imported in eclipse, and an automatic check is done on compile to ensure that the configuration is being followed.


### JavaDoc Comments

As per the example below, include the `JavadocType` and `JavadocMethod` accompained by `JavadocStyle`. We highly recommend including `PackageHtml`.

```xml
<!-- http://checkstyle.sf.net/config_javadoc.html -->
<module name="JavadocMethod"/>
<module name="JavadocType"/>
<module name="JavadocVariable"/>
```

### Naming Classes

As per the example below, include all standard naming conventions.  `AbstractClassName` (which enforces Abstract prefix or Factory suffix) and `ConstantName` (which enforces all CAPS) may be excluded.

```xml
<!-- http://checkstyle.sf.net/config_naming.html -->
<module name="LocalFinalVariableName"/>
<module name="LocalVariableName"/>
<module name="MethodName"/>
<module name="PackageName"/>
<module name="ParameterName"/>
<module name="StaticVariableName"/>
<module name="TypeName"/>
<module name="MemberName"/>
<!-- <module name="ConstantName"/> -->
```

### Importing Clases

As per the example below, include `AvoidStarImport`. Though it makes Java files long, it provides clear information about classes and their use. Further, include `IllegalImport`, `RedundantImport` and `UnusedImports`.

```xml
<!-- http://checkstyle.sf.net/config_import.html -->
<module name="AvoidStarImport"/>
<module name="IllegalImport"/>
<module name="RedundantImport"/>
<module name="UnusedImports"/>
```

### File Size

Include a methodLength check to ensure that files contain no more than 2000 lines and methods contain no more then 7 parameters.

```xml
<!-- http://checkstyle.sf.net/config_sizes.html -->
<module name="FileLength"/>
<module name="ParameterNumber"/>
```

### Whitespace

As per the example below, set the `tabWidth` property to 4.

```xml
<!-- http://checkstyle.sf.net/config_whitespace.html -->
<module name="EmptyForIteratorPad"/>
<module name="NoWhitespaceAfter"/>
<module name="NoWhitespaceBefore"/>
<module name="OperatorWrap"/>
<module name="TabCharacter"/>
<module name="WhitespaceAfter"/>
<module name="WhitespaceAround"/>
```

### Modifier order

To avoid redundant modifiers on interfaces and annotations, follow the order of the Java Language specification, as per the following example. 

```xml
<!-- http://checkstyle.sf.net/config_modifiers.html -->
<module name="ModifierOrder"/>
<module name="RedundantModifier"/>
```

### Block checks

As per the following example, ensure block checks are applied so that there are no empty blocks, and all blocks are contained within braces.

```xml
<\!-\- [http://checkstyle.sf.net/config_blocks.html] \-->
<module name="AvoidNestedBlocks"/>
<module name="EmptyBlock"/>
<module name="NeedBraces"/>
<module name="LeftCurly">
 <property name="option" value="nl"/>
</module>
<module name="RightCurly">
 <property name="option" value="alone"/>
</module>
```

### Coding

As per the following example, ensure your project meets these general coding conventions.

```xml
<\!-\- [http://checkstyle.sf.net/config_coding.html] \-->
<module name="AvoidInlineConditionals"/>
<module name="DoubleCheckedLocking"/>
<module name="EmptyStatement"/>
<module name="EqualsHashCode"/>
<module name="HiddenField"/>
<module name="IllegalInstantiation"/>
<module name="InnerAssignment"/>
<module name="MagicNumber"/>
<module name="MissingSwitchDefault"/>
<module name="RedundantThrows"/>
<module name="SimplifyBooleanExpression"/>
<module name="SimplifyBooleanReturn"/>
```

### Design

As per the the following example, ensure your project enforces encapsulation and adheres to standard coding design.

```xml
<\!-\- [http://checkstyle.sf.net/config_design.html] \-->
<module name="DesignForExtension"/>
<module name="FinalClass"/>
<module name="HideUtilityClassConstructor"/>
<module name="InterfaceIsType"/>
<module name="VisibilityModifier"/>
```

### Miscellaneous
As per the following example below, ensure your project meets these miscellaneous coding conventions.

```xml
<\!-\- [http://checkstyle.sf.net/config_misc.html] \-->
<module name="TodoComment"/>
<module name="UpperEll"/>
<module name="Translation"/> <\!-\- to the Checker \-->
```

## Source File Headers

Ensure that all source files contain the following header.

```java
/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
```

## Packages

| Convention           | Example      | Notes  |
|:--------------------------|:-------------|:-------------|
| Names representing packages should be in all lower case.	 | `org.mule.application`      | Package naming convention used by Sun for the Java core packages. The initial package name representing the domain name must be in lower case. |
| Package names should be singular rather than plural.	 | `org.mule.runtime.core.transformer`, `org.mule.runtime.core.processor`      |  |
| The 'org.mule.runtime.api' package tree should be used for all interfaces that make up the API/SPI.	 | `org.mule.runtime.core.api.lifecycle.Callable`      |  |


## Interfaces/Classes

| Convention           | Example      | Notes  |
|:--------------------------|:-------------|:-------------|
| Names representing types must be nouns and written in mixed case starting with upper case.	 | `MuleMessage`, `OrderService` | Common practice in the Java development community and also the type naming convention used by Sun for the Java core packages. |
| Interface and class names should avoid abbreviations, be descriptive and be camel-cased.	 | `interface OrderService` 
NOT: `interface OrderServ`      |  |
| Abstact classes should be prefixed with 'Abstract'.	 | `class AbstractOrderService implements OrderService` 
NOT `class OrderServiceAbs implements OrderService`  |  |
| Exception class names should be suffixed with 'Exception' | `EngineFailureException extends Exception` 
NOT: `EngineFailure extends Exception` | |
| Interface implementations should ideally be prefixed with something meaningful in the context. e.g. 'Simple', 'Generic' is possible. | `GenericLifeCycleManager`, `SingletonObjectFactory`  |  |
| Default interface implementations can be prefixed by 'Default'. | `class DefaultOrderService implements OrderService` 
NOT: `class OrderServiceImpl implements OrderService` | It is not uncommon to create a simplistic class implementation of an interface providing default behaviour to the interface methods. The convention of prefixing these classes by Default has been adopted by Sun for the Java library. |
| Test classes should be suffixed with 'TestCase' | `LoanBrokerMuleTestCase`  |  |

## Methods/Variables

| Convention           | Example      | Notes  |
|:--------------------------|:-------------|:-------------|
| Variable names must be in mixed case starting with lower case.	 | `message`, `newOrder` | Common practice in the Java development community and also the naming convention for variables used by Sun for the Java core packages. Makes variables easy to distinguish from types, and effectively resolves potential naming collision as in the declaration Line line; |
| Names representing constants (final variables) must be all uppercase using underscore to separate words.  | `MAX_ITERATIONS`, `COLOR_RED`  | Common practice in the Java development community and also the naming convention used by Sun for the Java core packages.  |
| Names representing methods must be verbs and written in mixed case starting with lower case. | `getName()`, `computeTotalWidth()`  | Common practice in the Java development community and also the naming convention used by Sun for the Java core packages and the JavaBean specification. This is identical to variable names, but methods in Java are already distinguishable from variables by their specific form. |
| Abbreviations and acronyms should not be uppercase when used as name. | `exportHtmlSource();` NOT: `exportHTMLSource();`
`openDvdPlayer(); ` NOT: `openDVDPlayer();` | Using all uppercase for the base name will give conflicts with the naming conventions given above. A variable of this type whould have to be named dVD, hTML etc. which obviously is not very readable. Another problem is illustrated in the examples above; When the name is connected to another, the readability is seriously reduced; The word following the acronym does not stand out as it should. |
| Underscores and other special characters should NOT be used in variable names, method names or class names | `private String name;` NOT `private String name_;` | Often private member variables are given an underscore '_' prefix to denote it's private member status. Mule does not use this convention as Java Editors make the status of variables know through color coding. |
| Generic variables should have the same name as their type. |  `void setTopic(Topic topic)` NOT: `void setTopic(Topic value)` NOT: `void setTopic(Topic aTopic)` NOT: `void setTopic(Topic t)` `void connect(Database database)` NOT: `void connect(Database db)` NOT: `void connect(Database oracleDB)` | Reduce complexity by reducing the number of terms and names used. Also makes it easy to deduce the type given a variable name only. If for some reason this convention doesn't seem to fit it is a strong indication that the type name is badly chosen. Non-generic variables have a role. These variables can often be named by combining role and type: Point startingPoint, centerPoint; Name loginName;|
|All names should be written in English.| | English is the preferred language for Mule development.|
| The terms get/set must be used where an attribute is accessed directly. | `employee.getName(); employee.setName(name); matrix.getElement(2, 4); matrix.setElement(2, 4, value);` | Common practice in the Java community and the convention used by Sun for the Java core packages and the JavaBean Specification. |
| `is` prefix should be used for boolean variables and methods.| `isSet`, `isVisible`, `isFinished`, `isFound`, `isOpen` | This is the naming convention for boolean methods and variables used by Sun for the Java core packages and the JavaBean specification. Using the is prefix solves a common problem of choosing bad boolean names like status or flag. isStatus or isFlag simply doesn't fit, and the programmer is forced to chose more meaningful names. |
| Negated boolean variable names must be avoided. | `boolean isError;` NOT: `boolean isNoError` boolean `isFound;` NOT: `isNotFound` | The problem arise when the logical not operator is used and double negative arises. It is not immediately apparent what !isNotError means.|
| Associated constants (final variables) should be prefixed by a common type name. | `final int COLOR_RED = 1; final int COLOR_GREEN = 2; final int COLOR_BLUE = 3;` | This indicates that the constants belong together, and what concept the constants represents. |

## Imports

In general, do not include star imports, and ensure that you sort all imports alphabetically, in ascending order in each group.

```java
org.mule
<blank Line>
com
<blank Line>
java
<blank Line>
javax
<blank Line>
<all other import sorted alphabetically>
```


# Mule XML Coding Conventions

## XML Formatting

| Concept    | Rule                                                                               |
|:-----------|:-----------------------------------------------------------------------------------|
| Wrapping   | Wrap lines at 120 characters. Wrap attributes only if they exceed 120 characters.  |
| Indenting  | Use four spaces for indenting, rather that tabs.                                   |
| Whitespace | Empty tags should not contain whitespace. Incorrect: `<tag />` Correct: `<tag/>`   |
| Aligning   | Align wrapped attributes with the first attribute on the previous line.            |

## XML Schema Conventions

| Schema Item                    | Convention                                                                         |
|:-------------------------------|:-----------------------------------------------------------------------------------|
| Simple Types and Complex Types | Use nouns for the names of simple and complex types. Use mixed case names, starting with lowercase. Always apply a `Type` suffix.  | `inboundRouterType` |
| Attributes                     | Use mixed case names for attributes, starting with lowercase. | `address` `name` `synchronous` |
| Element                        | Use lowercase for element names. Use a "-" separator between words, when necessary. | `inbound-router` `custom-transformer` |
| Groups                         | Use mixed case names for groups, starting with lowercase. Always apply a Group suffix.   | `inboundRouterGroup` `exceptionStrategiesGroup` |
| Namespaces                     | see example | http://www.mulesoft.org/schema/mule/${module}/${version} |
| Namespace prefixes             | see example | ${module} |
| Occurrence constraint          | Use occurrence constraints only when non-default values are required. | Note: For clarity's sake, include either all values, or just the non-default values. Given the number of places where minOccurs/maxOccurs attributes exists, it is tedious to complete all values. |


## XML Schema Best Practices

- Avoid restricting complex types; restricting simple types is acceptable.
- Use elements rather than attributes in the following circumstances:
    - you require complex types 
    - you do not have a requirement to specify valid combinations (for example, with choice)
    - you must accommodate future extensibility
- Keep schema restrictive but extensible
- Do not use unbound choice model groups, as per the example below.

```xml
<choice minOccurs="0" max Occurs="unbounded">
    ...
</choice>
```

Always order elements using `<sequence/>`. 
Specify occurrence constraints only on references to groups, rather than groups.

## Resources

Access [xFront Best Practices](http://www.xfront.com/BestPracticesHomepage.html) for more details.
