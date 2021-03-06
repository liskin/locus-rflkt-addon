/* Copyright (C) 2016 Tomáš Janoušek
 * This file is a part of locus-rflkt-addon.
 * See the COPYING and LICENSE files in the project root directory.
 */

lazy val commonSettings = Def.settings(
  platformTarget in Android := "android-23",
  targetSdkVersion in Android := "23",
  minSdkVersion in Android := "18",

  javacOptions ++= Seq("-source", "1.7", "-target", "1.7"),
  scalaVersion := "2.11.8",
  scalacOptions in Compile ++= Seq("-explaintypes", "-unchecked", "-feature", "-deprecation", "-target:jvm-1.7")
)

lazy val root = project.in(file("."))
.enablePlugins(AndroidApp)
.dependsOn(noAnalytics)
.settings(
  name := "locus-rflkt-addon",

  commonSettings,
  //protifySettings,

  updateCheck in Android := {}, // disable update check
  proguardCache in Android ++= Seq(
    "macroid",
    "org.slf4j",
    "org.log4s",
    "com.google",
    "com.wahoofitness",
    "com.dsi",
    "com.garmin",
    "android.support",
    "locus"
  ),

  proguardVersion := "5.3.2",
  proguardOptions in Android ++= Seq("-keepattributes Signature", "-ignorewarnings"),
  proguardConfig in Android := {
    // This is probably wrong
    // (https://github.com/pfn/android-sdk-plugin/issues/242) but it
    // seems to work fine, and even if it didn't, it doesn't matter as
    // AndroidManifest changes require clean/reload anyway.
    val debug = (apkbuildDebug in Android).value()
    (proguardConfig in Android).value filter {
      case "-dontobfuscate" if !debug => false
      case "-dontoptimize" if !debug => false
      case _ => true
    }
  },

  mergeManifests in Android := false,

  resolvers += "jcenter" at "http://jcenter.bintray.com",

  libraryDependencies += aar("org.macroid" %% "macroid" % "2.0"),
  dependencyOverrides += "com.android.support" % "support-v4" % "23.4.0",
  libraryDependencies += "com.android.support" % "appcompat-v7" % "23.4.0",
  libraryDependencies += "org.log4s" %% "log4s" % "1.3.4",
  libraryDependencies += "org.slf4j" % "slf4j-android" % "1.7.22",
  libraryDependencies += "de.psdev.licensesdialog" % "licensesdialog" % "1.8.1",
  libraryDependencies += "com.github.ghik" % "silencer-lib" % "0.4",
  libraryDependencies += "com.asamm" % "locus-api-android" % "0.2.7",

  addCompilerPlugin("com.github.ghik" % "silencer-plugin" % "0.4")
)

lazy val noAnalytics = project.in(file("deps/NoAnalytics/NoAnalytics"))
.enablePlugins(AndroidJar)
.settings(
  name := "NoAnalytics",
  commonSettings,
  antLayoutDetector in Android := ()
)
