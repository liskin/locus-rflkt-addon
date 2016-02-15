name := "locus-rflkt-addon"

import android.Keys._
android.Plugin.androidBuild
protifySettings

javacOptions ++= Seq("-source", "1.7", "-target", "1.7")
scalaVersion := "2.11.7"
scalacOptions in Compile ++= Seq("-explaintypes", "-unchecked", "-feature", "-deprecation")

updateCheck in Android := {} // disable update check
proguardCache in Android ++= Seq("org/scaloid", "org.scaloid") // FIXME: just use android-sdk-plugin 1.5.17-SNAPSHOT instead

proguardOptions in Android ++= Seq("-dontobfuscate", "-dontoptimize", "-keepattributes Signature", "-printseeds target/seeds.txt", "-printusage target/usage.txt"
  , "-dontwarn scala.collection.**" // required from Scala 2.11.4
  , "-dontwarn org.scaloid.**" // this can be omitted if current Android Build target is android-16
)

libraryDependencies += "org.scaloid" %% "scaloid" % "4.1"
libraryDependencies += "com.android.support" % "support-v4" % "23.1.1"

run <<= run in Android
install <<= install in Android
