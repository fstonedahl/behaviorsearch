name := "behaviorsearch"

organization := "bsearch"

scalaVersion := "2.12.0"

val netLogoVersion = settingKey[String]("active version of NetLogo")

netLogoVersion := "6.0.0-BETA2"

resolvers += Resolver.bintrayRepo("content/netlogo", "NetLogo-JVM")

libraryDependencies ++= Seq(
  "jfree"     % "jfreechart" % "1.0.13",
  "jfree"     % "jcommon"    % "1.0.16",
  "args4j"    % "args4j"     % "2.0.12",
  "org.nlogo" % "netlogo"    % netLogoVersion.value intransitive,
  "org.ow2.asm" % "asm-all" % "5.0.4" % "test",
  "org.picocontainer" % "picocontainer" % "2.13.6" % "test",
  "org.parboiled" %% "parboiled" % "2.1.3" % "test",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4" % "test",
  "com.novocode" % "junit-interface" % "0.11" % "test"
)

artifactName := { (sv: ScalaVersion, module: ModuleID, artifact: Artifact) =>
    artifact.name + "." + artifact.extension
}

javacOptions in Compile ++= List("-g", "-deprecation", "-target", "1.8", "-source", "1.8")

javacOptions in Test++= List("-g", "-deprecation", "-target", "1.8", "-source", "1.8")

javaSource in Compile := baseDirectory.value / "src"

excludeFilter in Compile in unmanagedSources := "*test*"

javaSource in Test := baseDirectory.value / "src"

excludeFilter in Test in unmanagedSources := HiddenFileFilter

includeFilter in Test in unmanagedSources := "*Test.java"

testOptions in Test := Seq(Tests.Argument(TestFrameworks.JUnit, "-a"))

fork in Test := true

crossPaths := false
