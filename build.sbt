/* Copyright (C) 2016 Tomáš Janoušek
 * This file is a part of locus-rflkt-addon.
 * See the COPYING and LICENSE files in the project root directory.
 */

lazy val commonSettings = Def.settings(
  platformTarget in Android := "android-23",
  targetSdkVersion in Android := "23",
  minSdkVersion in Android := "18",

  javacOptions ++= Seq("-source", "1.7", "-target", "1.7"),
  scalaVersion := "2.11.7",
  scalacOptions in Compile ++= Seq("-explaintypes", "-unchecked", "-feature", "-deprecation", "-target:jvm-1.7")
)

lazy val root = project.in(file("."))
.androidBuildWith(noAnalytics, locusApiAndroid)
.settings(
  name := "locus-rflkt-addon",

  commonSettings,
  protifySettings,

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

  proguardOptions in Android ++= Seq("-keepattributes Signature"),
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

  libraryDependencies += aar("org.macroid" %% "macroid" % "2.0.0-20150427"),
  libraryDependencies += "com.android.support" % "appcompat-v7" % "23.1.1",
  libraryDependencies += "org.log4s" %% "log4s" % "1.2.1",
  libraryDependencies += "org.slf4j" % "slf4j-android" % "1.7.18",
  libraryDependencies += "de.psdev.licensesdialog" % "licensesdialog" % "1.8.0"
)

lazy val noAnalytics = project.in(file("deps/NoAnalytics/NoAnalytics"))
.androidBuildWith()
.settings(
  name := "NoAnalytics",
  commonSettings,
  antLayoutDetector in Android := (),
  buildJar
)

lazy val locusApi = project.in(file("deps/locus-api"))
.androidBuildWith()
.settings(
  name := "locus-api",
  commonSettings,
  antLayoutDetector in Android := (),
  projectLayout in Android := new ProjectLayout.Ant(baseDirectory.value),
  buildJar,
  noManifest,
  lintEnabled in Android := false // FIXME
)

lazy val locusApiAndroid = project.in(file("deps/locus-api-android"))
.androidBuildWith(locusApi)
.settings(
  name := "locus-api-android",
  commonSettings,
  antLayoutDetector in Android := (),
  buildJar
)

lazy val noManifest = Def.settings(
  projectLayout in Android :=
    new ProjectLayout.Wrapped((projectLayout in Android).value) {
      // use the generated manifest for rGenerator
      override def manifest: File = outputLayout.value(this).processedManifest
    },
  compile in Compile := {
    // generate the manifest in time for rGenerator
    (processManifest in Android).value
    (compile in Compile).value
  }
)
