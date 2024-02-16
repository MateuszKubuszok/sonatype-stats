# sonatype-stats

This is the fork of the [sonatype-stats](https://github.com/alexarchambault/sonatype-stats) utility
which has a goal of:

 - [x] migrating the tool from Ammonite to Scala CLI
 - [x] migrating from Scala 2.12 to Scala 3
 - [x] migrating from Circe to Jsoniter Scala
 - [x] migrating to newer version of STTP
 - [x] migrating charts to Chart.js
 - [x] migrating from Travis CI to GH Actions (WIP!)

## Running locally

Install Scala CLI. Then run:

```scala
SONATYPE_PROJECT=[project name] SONATYPE_USERNAME=[username] SONATYPE_PASSWORD='password' scala-cli run .
```

putting the right values for your project and Sonatype user. The data will be generated in `data` directory.

## Running in scheduled GitHub Action

We can create a GH Action which could publish Sonatype Stats on GitHub Pages in a dedicated repository:

 * create a repository dedicated for Sonatype statistics - they will be visible on its GitHub Pages site
 * obtain Sonatype credentials (instead of username you can create [User Token](https://central.sonatype.org/publish/generate-token/))
   * setup `SONATYPE_USERNAME` secret
   * setup `SONATYPE_PASSWORD` secret
 * find which project ID you want to use (you can check what projects you see on e.g. a Central Statistics after you log in)
   * setup `SONATYPE_PROJECT` secret
 * create GitHub [Access Token](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens)
   with an access to the repository (we'll be using [Publish to GitHub Pages](https://github.com/marketplace/actions/publish-to-github-pages) action)
   * in Fine-graned tokens you need to grant "Read access to metadata" and "Read and Write access to code and pages" just for the repository which
     will host stats on its GH Pages
 * finally, create in your repository file `.github/workflows/sonatype-stats.yml` with the content like below:

```yml
name: Publish Sonatype Stats to GitHub Pages

on:
  push:
    branches:
      # update index.html also if anything was modified in the workflow
      - master
  schedule:
    # * is a special character in YAML so you have to quote this string
    - cron:  '0 0 7,15 * *'

jobs:
  publish:
    runs-on: ubuntu-latest
    steps:
      - name: Check out
        uses: actions/checkout@v2

      - name: Cache fetched data
        uses: actions/cache@v4
        with:
          path: data
          key: setup-1

      - name: Fetch and render Sonatype Stats
        uses: MateuszKubuszok/sonatype-stats@master
        with:
          sonatype-project: ${{ vars.SONATYPE_PROJECT }}
          sonatype-username: ${{ secrets.SONATYPE_USERNAME }}
          sonatype-password: ${{ secrets.SONATYPE_PASSWORD }}

      - name: Publish generated content to GitHub Pages
        uses: tsunematsu21/actions-publish-gh-pages@v1.0.2
        with:
          dir: data
          branch: gh-pages
          token: ${{ secrets.ACCESS_TOKEN }}
```

This will create a job that is running every 1st and 15th of the month - once a month ends Sonatype triggers a workflow that computes stats for it.
Usually it takes several days before they are avaiable (often around a week), so there is no point in running this action very often.
(Stats once downloaded are cached, so we only have to add missing ones).

Once it finishes running, don't forget to enable GitHub Pages for your repository.

See example for [Scalaland.io](https://scalalandio.github.io/sonatype-stats/).

> You can tweak this setup as it only shows the most basic use case.
