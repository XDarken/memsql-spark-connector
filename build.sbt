import UnidocKeys._

lazy val sparkVersion = "1.4.1"
lazy val mysqlConnectorVersion = "5.1.34"
lazy val akkaVersion = "2.3.9"
lazy val sprayVersion = "1.3.2"
lazy val scoptVersion = "3.2.0"
lazy val scalatestVersion = "2.2.5"

lazy val assemblyScalastyle = taskKey[Unit]("assemblyScalastyle")
lazy val testScalastyle = taskKey[Unit]("testScalastyle")

lazy val commonSettings = Seq(
  organization := "com.memsql",
  version := "0.2.3",
  scalaVersion := "2.10.5",
  assemblyScalastyle := org.scalastyle.sbt.ScalastylePlugin.scalastyle.in(Compile).toTask("").value,
  assembly <<= assembly dependsOn assemblyScalastyle,
  testScalastyle := org.scalastyle.sbt.ScalastylePlugin.scalastyle.in(Test).toTask("").value,
  (test in Test) <<= (test in Test) dependsOn testScalastyle,
  publishTo := Some(Resolver.file("file",  new File(Path.userHome.absolutePath+"/.m2/repository")))
)

lazy val connectorLib = (project in file("connectorLib")).
  settings(commonSettings: _*).
  settings(
    name := "MemSQLRDD",
    libraryDependencies  ++= Seq(
      "org.apache.spark" %% "spark-core" % sparkVersion % Provided,
      "org.apache.spark" %% "spark-sql" % sparkVersion  % Provided,
      "mysql" % "mysql-connector-java" % mysqlConnectorVersion,
      "org.scalatest" %% "scalatest" % scalatestVersion % Test
    ),
    autoAPIMappings := true,
    apiMappings ++= {
      def findManagedDependency(organization: String, name: String): Option[File] = {
        (for {
          entry <- (fullClasspath in Runtime).value ++ (fullClasspath in Test).value
          module <- entry.get(moduleID.key) if module.organization == organization && module.name.startsWith(name)
        } yield entry.data).headOption
      }
      val links = Seq(
        findManagedDependency("org.apache.spark", "spark-core").map(d => d -> url(s"https://spark.apache.org/docs/$sparkVersion/api/scala/")),
        findManagedDependency("org.apache.spark", "spark-sql").map(d => d -> url(s"https://spark.apache.org/docs/$sparkVersion/api/scala/"))
      )
      links.collect { case Some(d) => d }.toMap
    }
  )

lazy val etlLib = (project in file("etlLib")).
  dependsOn(connectorLib).
  settings(commonSettings: _*).
  settings(
    name := "MemSQLETL",
    libraryDependencies  ++= Seq(
      "io.spray" %% "spray-json" % sprayVersion,
      "org.apache.spark" %% "spark-streaming" % sparkVersion % Provided,
      "org.apache.spark" %% "spark-sql" % sparkVersion  % Provided,
      "org.scalatest" %% "scalatest" % scalatestVersion % Test
    ),
    autoAPIMappings := true,
    apiMappings ++= {
      def findManagedDependency(organization: String, name: String): Option[File] = {
        (for {
          entry <- (fullClasspath in Runtime).value ++ (fullClasspath in Test).value
          module <- entry.get(moduleID.key) if module.organization == organization && module.name.startsWith(name)
        } yield entry.data).headOption
      }
      val links = Seq(
        findManagedDependency("org.apache.spark", "spark-core").map(d => d -> url(s"https://spark.apache.org/docs/$sparkVersion/api/scala/")),
        findManagedDependency("org.apache.spark", "spark-streaming").map(d => d -> url(s"https://spark.apache.org/docs/$sparkVersion/api/scala/")),
        findManagedDependency("org.apache.spark", "spark-sql").map(d => d -> url(s"https://spark.apache.org/docs/$sparkVersion/api/scala/"))
      )
      links.collect { case Some(d) => d }.toMap
    }
  )

lazy val jarInspector = (project in file("jarInspector")).
  dependsOn(etlLib).
  settings(commonSettings: _*).
  settings(
    name := "jarInspector",
    libraryDependencies  ++= Seq(
      "org.apache.spark" %% "spark-core" % sparkVersion % Provided,
      "io.spray" %% "spray-json" % sprayVersion,
      "com.github.scopt" %% "scopt" % scoptVersion,
      "org.reflections" % "reflections" % "0.9.10"
    )
  )

lazy val interface = (project in file("interface")).
  dependsOn(connectorLib).
  dependsOn(etlLib).
  dependsOn(jarInspector).
  settings(commonSettings: _*).
  settings(
    name := "MemSQLSparkInterface",
    parallelExecution in Test := false,
    libraryDependencies ++= {
      Seq(
        "io.spray" %% "spray-json" % sprayVersion,
        "io.spray" %% "spray-can" % sprayVersion,
        "io.spray" %% "spray-routing" % sprayVersion,
        "com.typesafe.akka" %% "akka-actor" % akkaVersion,
        "com.github.scopt" %% "scopt" % scoptVersion,
        "mysql" % "mysql-connector-java" % mysqlConnectorVersion,
        "org.eclipse.jetty" % "jetty-servlet" % "8.1.14.v20131031" % Provided,
        "org.apache.spark" %% "spark-core" % sparkVersion % Provided,
        "org.apache.spark" %% "spark-sql" % sparkVersion  % Provided,
        "org.apache.spark" %% "spark-streaming" % sparkVersion % Provided,
        "org.apache.spark" %% "spark-streaming-kafka" % sparkVersion exclude("org.spark-project.spark", "unused"),
        "org.scalatest" %% "scalatest" % scalatestVersion % Test,
        "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
        "io.spray" %% "spray-testkit" % sprayVersion % Test exclude("org.scalamacros", "quasiquotes_2.10.3"),
        "org.apache.commons" % "commons-csv" % "1.2"
      )
    }
  )

lazy val examples = (project in file("examples")).
  dependsOn(interface).
  settings(commonSettings: _*).
  settings(
    name := "examples",
    libraryDependencies ++= {
      Seq(
        "mysql" % "mysql-connector-java" % mysqlConnectorVersion,
        "org.apache.spark" %% "spark-core" % sparkVersion % Provided,
        "org.apache.spark" %% "spark-streaming" % sparkVersion % Provided,
        "org.apache.spark" %% "spark-sql" % sparkVersion  % Provided
      )
    }
  )

lazy val tests = (project in file("tests")).
  dependsOn(connectorLib).
  dependsOn(etlLib).
  dependsOn(interface).
  dependsOn(examples).
  settings(commonSettings: _*).
  settings(
    name := "tests",
    parallelExecution in Test := false,
    libraryDependencies ++= {
      Seq(
        "mysql" % "mysql-connector-java" % mysqlConnectorVersion,
        "org.apache.spark" %% "spark-core" % sparkVersion % Provided,
        "org.apache.spark" %% "spark-streaming" % sparkVersion % Provided,
        "org.apache.spark" %% "spark-sql" % sparkVersion  % Provided
      )
    }
  )

lazy val root = (project in file(".")).
  dependsOn(connectorLib).
  dependsOn(etlLib).
  dependsOn(interface).
  settings(commonSettings: _*).
  settings(unidocSettings: _*).
  settings(
    name := "MemSQL",
    libraryDependencies  ++= Seq(
      "org.apache.spark" %% "spark-core" % sparkVersion % Provided,
      "org.apache.spark" %% "spark-sql" % sparkVersion  % Provided,
      "org.apache.spark" %% "spark-streaming" % sparkVersion % Provided,
      "mysql" % "mysql-connector-java" % mysqlConnectorVersion
    ),
    unidocProjectFilter in (ScalaUnidoc, unidoc) := inAnyProject -- inProjects(tests, jarInspector)
  )
