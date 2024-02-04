# sonatype-stats

This is the fork of the [sonatype-stats](https://github.com/alexarchambault/sonatype-stats) utility
which has a goal of:

 - [x] migrating the tool from Ammonite to Scala CLI
 - [x] migrating from Scala 2.12 to Scala 3
 - [x] migrating from Circe to Jsoniter Scala
 - [x] migrating to newer version of STTP
 - [ ] migrating from Travis CI to GH Actions

## Running locally

Install Scala CLI. Then run:

```scala
SONATYPE_PROJECT=[project name] SONATYPE_USERNAME=[username] SONATYPE_PASSWORD='password' scala-cli run .
```

putting the right values for your project and Sonatype user. The data will be generated in `data` directory.

 ## TODO

 - [x] detect automatically when the project was first published to NOT hardcode the first date
 - [ ] generate plots like the original
 - [ ] create GH Action to automatically update stats every one in a while
