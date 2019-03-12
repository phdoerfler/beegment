# Auto Refresh for Beeminder's Trello Integration

## How to Run

`sbt run`

## How to Test

`sbt test`

### Authorization

To run the tests you first need to provide an auth_token by supplying an implicit instance of `AuthToken`:

```scala
package io.doerfler

package object echobee {
  implicit def at: AuthToken = AuthToken("here_goes_your_token")
}
```

This would go into `src/test/scala/io/doerfler/echobee/package.scala`.
Yeah it's not pretty but it's checked at compile time so at least there is that.