[![Build Status](https://travis-ci.com/phdoerfler/beegment.svg?branch=master)](https://travis-ci.com/phdoerfler/beegment)

# Additions to the Beeminder API

## Feature(s)

- Auto Refresh for Beeminder's Trello Integration

## How to Run

Use `sbt run` or `sbt ~reStart` during development.

To create a distributable version, run `sbt pack`, then start the server with `target/pack/bin/beegment`.