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
an implementation detail, and may be subject to change. Using snabbdom features
not explicitly exposed by dumdom is **not** recommended.

**dumdom** aims to be finished, stable, and worthy of your trust. Breaking
changes will never be intentionally introduced to the codebase. For this reason,
dumdom does not adhere to the "semantic" versioning scheme: releases are
just numbered sequentially.

## Table of contents

* [Rationale](#rationale)
* [Limitations](#limitations)
* [Differences from Quiescent](#differences-from-quiescent)
* [Using with Devcards](#using-with-devcards)
* [Contribute](#contribute)
* [Documentation](#documentation)
* [License](#license)

## Rationale

Of the many possible options, [Quiescent](https://github.com/levand/quiescent)
is to me the perfect expression of "React in ClojureScript". It's simple,
light-weight, does not allow component-local state, and pitches itself as
strictly a rendering library, not a state management tool or UI framework.

While Quiescent has been done (as in "complete") for a long time, it is built on
React, which is on a cycle of recurring "deprecations" and API changes, making
it hard to keep Quiescent up to date with relevant security patches etc. At the
same time, React keeps adding features which are of no relevance to the API
Quiescent exposes, thus growing the total bundle size for no advantage to
its users.

**dumdom** provides the same API as that of Quiescent, but does not depend on
React. It aims to be as stable and complete as Quiescent, but still be able to
ship occasional security patches as they are made to the underlying virtual DOM
library. **dumdom** aims to reduce the amount of churn in your UI stack.

## Limitations

Because **dumdom** is not based on React, you opt out of the "React ecosystem"
entirely by using it. If you depend on a lot of open source/shared React
components, or other React-oriented tooling, **dumdom** might not be the best
fit for you.

Because **dumdom** does not offer any kind of component local state, it cannot
be used as a wholistic UI framework - it's just a rendering library. It does not
come with any system for routing, dispatching actions, or managing state (either
inside or outside of components), and is generally a batteries-not-included
tool. I consider this a strength, others may see it differently.

## Differences from Quiescent

Dumdom strives to be API compliant with Quiescent to the degree that it should
be a drop-in replacement for Quiescent in any project that does not rely
explicitly on any React APIs or third-party components. It does not necessarily
commit to all the same restrictions that the Quiescent API imposes. The
following is a list of minor details between the two:

- Quiescent does not allow the use of `:on-render` along with either of
  `:on-mount` and `:on-update`. Dumdom acknowledges that some components will
  implement `:on-render` *and* `:on-mount` or `:on-update`, and allows this.
- Dumdom doesn't really care about `TransitionGroup`. You are free to use them,
  but the animation callbacks will work equally well outside `TransitionGroup`.
  This may cause breakage in some cases when porting from Quiescent to Dumdom.
  The risk is pretty low, and the upside is significant enough to allow Dumdom
  to take this liberty.

## Using with Devcards

[Devcards](https://github.com/bhauman/devcards) is a system for rendering React
components in isolation. Because **dumdom** components are not React components,
they need some wrapping for Devcards to make sense of them. This is what the
`dumdom.devcards` namespace is for:

```clj
(require '[dumdom.devcards :refer-macros [defcard]])

(defcard my-dumdom-card
  (MyDumDomComponent {:value 0}))
```

`dumdom.devcards.defcard` works exactly the same as
[devcards.core.defcard](http://rigsomelight.com/devcards/#!/devdemos.defcard_api).

## Contribute

Feel free to report bugs and, even better, provide bug fixing pull requests!
Make sure to add tests for your fixes, and make sure the existing ones stay
green before submitting fixes.

If you have ideas for new features, please open an issue to discuss the idea and
the API before implementing it to avoid putting lots of work into a pull request
that might be rejected. I intend to keep **dumdom** a focused package, and don't
want it to accrete a too wide/too losely coherent set of features.

TODO: How to run tests

## Documentation

**dumdom** is currently under development. Documentation will follow when the
library is functional and usable.

## License

Copyright © 2018 Christian Johansen

Distributed under the Eclipse Public License either version 1.0 or (at your
option) any later version.
