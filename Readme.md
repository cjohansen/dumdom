# dumdom - The dumb DOM component library

**dumdom** is a component library that renders (and re-renders) immutable data
efficiently. It delivers on the basic value proposition of React and its peers
while eschewing features like component local state and object oriented APIs,
and embracing ClojureScript features like immutable data structures.

**dumdom** aims to be API compatible with
[Quiescent](https://github.com/levand/quiescent/), and can be used as a drop-in
replacement for it so long as you don't use React features directly.

**dumdom** is currently a wrapper for
[Snabbdom](https://github.com/snabbdom/snabbdom), but that should be considered
an implementation detail, and may be subject to change.

## Documentation

**dumdom** is currently under development. Documentation will follow when the
library is functional and usable.

## Differences from Quiescent

Dumdom strives to be API compliant with Quiescent to the degree that it should
be a drop-in replacement for Quiescent in any project that does not rely
explicitly on any React APIs or third-party components. It does not necessarily
commit to all the same restrictions that the Quiescent API imposes. The
following is a list of minor details between the two:

- Quiescent does not allow the use of `:on-render` along with either of
  `:on-mount` and `:on-update`. Dumdom acknowledges that some components will
  implement `:on-render` *and* `:on-mount` or `:on-update`, and allows this.

## License

Copyright © 2018 Christian Johansen

Distributed under the Eclipse Public License either version 1.0 or (at your
option) any later version.
