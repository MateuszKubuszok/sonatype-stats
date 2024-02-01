# sonatype-stats

This is the fork of the [sonatype-stats](https://github.com/alexarchambault/sonatype-stats) utility
which has a goal of:

 - [x] migrating the tool from Ammonite to Scala CLI
 - [x] migrating from Scala 2.12 to Scala 3
 - [x] migrating from Circe to Jsoniter Scala
 - [x] migrating to newer version of STTP
 - [ ] migrating from Travis CI to GH Actions

 ## TODO

 - [ ] detect automatically when the project was first published to NOT hardcode the first date
 - [ ] generate plots like the original
 - [ ] create GH Action to automatically update stats every one in a while
