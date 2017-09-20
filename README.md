# SPManager

Licensed under GNU GPL v3, for more information please see LICENSE file.

## SPManager - Introduciton

SPManager is an android SDK designed to take the "burden" of creating and maintining shared preferences from the developer, allowing them to mark which changes they are interested in and which methods should be called when those changes happen.
SPManager is designed with ease of use in mind and will generate your own personal SPManager file, this file will be your ticket into your shared preferences.

## SPManager - How it works and how to use it

### TL;DR
#### Steps to make this work:
+ Integrating the SDK:<br>
Mark a class with `@SPManager`, the class needs to be only singleton, doesn't matter what code you write inside it as long as there is a `private static XXX instance`, `private XXX(){/*...*/}` and `public static XXX getInstance(){/*...*/}`. This generates a default template of SPManager that you can use (instructions on how to use come later on). You must build the project in order for the class to be generated.
+ Receiving updates:<br>Mark a class with `@SPListener`, that class must not have any super-class besides object (interfaces are okay). Inside the class there has to be at least one method annotated with `@SPUpdateTarget` that receives a `Conext`, `String` and `Object` in that order and no other parameters. Any time there is an update in SPManager then the method wil be invoked.
+ Receving specific updates:<br>Go to an annotated method (`@SPUpdateTarget`) and open brackets, inside of the brackets write `keys={/* The keys you want to receive*/}`. Any key you write down will cause the method to be invoked, any key that isn't written will not invoke the method.
+ Updateing values:<br>After integrating the SDK, you must instantiate the class with `SharedPreferences` so that the class knows to which SP it needs to update values, this is done by: `SPManager.getInstance(/*some SP*/);`. In order to update values you will need to do the following: `SPManager.getInstance(/*some SP or null if it is already initialized*/).update("Somekey", "Somevalue");`
+ Gettings values:<br>If you don't want to use `@SPListener` you can access a specific value like so: `SPManager.getInstance(/*some SP or null if it is already initialized*/).get("Somekey");`


### Creating your manager
SPManager uses Annotation Processing much like Dagger, but to a lesser degree (after all I am not Google).
SPManager's use is really simple; you first need to designated a singleton class (in this case we will call the class `SPModule`) to be marked with `@SPManager`, this class doesn't have to contain any code, except for three things:
+ Our instance:
```Java
private static SPMoudle mInstance;
```
+ Our getter:
```Java
public static SPModule getInstance(){/*...*/}
```
+ Our constructor:
```Java
private SPModule(){/*...*/}
```
_**Please Note That all of these things are a must, and the program will not compile without them.**_

> Once you have done that you can feel free to build your project.<br>
> ***Congratulations***! You now have your own monitored and functional SharedPreferences Manager. Neet, right?<br>
> Well as much as I love to compliment myself, it isn't over yet.

Right now, what you possess is a pre-built SharedPreferences Manager class I have used in the past and it's pretty simple, something your average Joe can do. What comes now is the cooler part,  _**Listening For Updates!**_

### Creating Your Listeners
In order to listen for updates in the SharedPreferences, you will need to define a class (in this call we will call the class `SPUpdater`) to be marked with `@SPListener`, this class ***MUST*** contain a method annotated with `@SPUpdateTarget` that receives a context, string and object in that order, without one such method the app won't go through compilation!

Here's an example of how this is done:
```java
  @SPUpdateTarget
  public void onSPUpdate(Context context, String str, Object obj){/*...*/}
```

_*** Please Note: If your arguments aren't in the correct order, aren't of the correct type, or you do not have a method annotated with `@SPUpdateTarget` the app won't go through compilation *** _

Now what about specific key updates?
Good question hypothetical person!
By default all methods annotated with `@SPUpdateTarget` will receive all key updates, meaning as long as you change a key, the method will run. This means you can have 5 methods doing different things and they will all run one after the other (order of execution is not yet a feature but it will be). This also means that you can have two methods, one handling all key updates, and one handling specific key updates. You would go about doing this in the following manor:

You would define another method to handle updates, but this time, in the `@SPUpdateTarget` you will use the `keys` parameter in order to state the specific keys that will cause this method to be executed.

```java
  @SPUpdateTarget ( keys = {"randomKey", "AnotherKey"} )
  public void onSPSpecificUpdate(Context context, String str, Object obj){/*...*/}
```

And once again, once you update the SharedPreferences with a key specified in the method, only then will it execute.

> **Congratulations!** You now know how to use my SDK!
> But is this the end?
> Not at all.

### Error Handling
This is a relativly slim section, since I didn't put a lot of features regarding errors.
If your code has caused an error, it won't crash your application I made sure of that (with regards to using my SDK, I don't know how you handle exceptions elsewhere). You will be prompted by a nice little information chunk in your logcat stating that looks like this:

```
09-17 00:11:41.172 16458-16458/com.sharedpreferencesmanagerdemo E/SPManager: One of the UpdateTarget methods has an internal error, meaning something isn't right in the code you wrote.
                                                                             Class name: com.sharedpreferencesmanagerdemo.SPUpdater, method: onUpdate
09-17 00:11:41.174 16458-16458/com.sharedpreferencesmanagerdemo A/SPManager: The error;
```

### How to actually use it
This is the last section, and is the sum of everything we did so far.
In order to use the generated manager you will need to do the following:
```java
  SPManager manager = SPManager.getInstance(XXX);
```
In this section XXX refers to a SharedPreferences object that you pass to the class, in order to know with what SP should the class interact with.
Once you have the instance, you can update the values:
```java
  manager.update(this, "randomKey", 42);
```
This will set the value of `randomKey` to 42 and as a result will invoke both `onSPSpecificUpdate` and `onSPUpdate`.
```java
  manager.update(this, "r1adls", 65);
```
This will set the value of `r1adls` to 65 and as a result will invoke only `onSPUpdate`.

You can also get a value from the SP.
```java
  manager.get("randomKey"); // returns 42.
```

##  SPManager integration with gradle
To include the sdk in your projects, add the following to `build.gradle`:

>compile 'com.aongoltzcrank:sharedpreferencesmanager:1.0'


## Bugs
If you encounter any bugs, please write them as an issue, append a code example for replication, and even a screenshot of the issue.

