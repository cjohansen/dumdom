# dumdom - The dumb DOM component library

**dumdom** is a component library that renders (and re-renders) immutable data
efficiently. It delivers on the basic value proposition of React and its peers
while eschewing features like component local state and object oriented APIs,
and embracing ClojureScript features like immutable data structures.

**dumdom** is API compatible with
[Quiescent](https://github.com/levand/quiescent/), and can be used as a drop-in
replacement for it so long as you don't use React features directly.

**dumdom** is currently a wrapper for
[Snabbdom](https://github.com/snabbdom/snabbdom), but that should be considered
an implementation detail, and may be subject to change. Using snabbdom features
not explicitly exposed by dumdom is **not** recommended.

**dumdom** aims to be finished, stable, and worthy of your trust. Breaking
changes will never be intentionally introduced to the codebase. For this reason,
dumdom does not adhere to the "semantic" versioning scheme.

**dumdom** supports server-side rendering to strings.

## Table of contents

* [Install](#install)
* [Example](#example)
* [Rationale](#rationale)
* [Limitations](#limitations)
* [Differences from Quiescent](#differences-from-quiescent)
* [Using with Devcards](#using-with-devcards)
* [Contribute](#contribute)
* [Documentation](#documentation)
* [Changelog](#changelog)
* [License](#license)

## Install

With tools.deps:

```clj
cjohansen/dumdom {:mvn/version "2018.12.22"}
```

With Leiningen:

```clj
[cjohansen/dumdom "2018.12.22"]
```

## Example

```clj
(require '[dumdom.core :as dumdom :refer [defcomponent]]
         '[dumdom.dom :as d])

(defcomponent Heading
  :on-render (fn [dom-node val old-val])
  [data]
  (d/h2 {:style {:background "#000"}} (:text data)))

(defcomponent Page [data]
  (d/div {}
    (Heading (:heading data))
    (d/p {} (:body data))))

(dumdom/render
 (Page {:heading {:text "Hello world"}
        :body "This is a web page"})
 (js/document.getElementById "app"))
```

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

Dumdom requires Devcards 0.2.6.

## Contribute

Feel free to report bugs and, even better, provide bug fixing pull requests!
Make sure to add tests for your fixes, and make sure the existing ones stay
green before submitting fixes.

```sh
make test
```

You can also run the tests in a browser with figwheel, which might be more
useful during development:

```sh
clojure -A:dev
```

Then open [http://localhost:9500/figwheel-extra-main/tests](http://localhost:9500/figwheel-extra-main/tests).

If you have ideas for new features, please open an issue to discuss the idea and
the API before implementing it to avoid putting lots of work into a pull request
that might be rejected. I intend to keep **dumdom** a focused package, and don't
want it to accrete a too wide/too losely coherent set of features.

## Documentation

### `(dumdom.core/render component element)`

Render the virtual DOM node created by the component into the specified DOM
element.

### `(dumdom.core/component render-fn [opt])`

Returns a component that uses the provided function for rendering. The resulting
component will only call through to its rendering function when called with data
that is different from the data that produced the currently rendered version of
the component.

The rendering function can be called with any number of arguments, but only the
first one will influence rendering decisions. You should call the component with
a single immutable value, followed by any number of other arguments, as desired.
These additional constant arguments are suitable for passing messaging channels,
configuration maps, and other utilities that are constant for the lifetime of
the rendered element.

The optional opts argument is a map with additional properties:

`:on-mount` - A function invoked once, immediately after initial rendering. It
is passed the rendered DOM node, and all arguments passed to the render
function.

`:on-update` - A function invoked immediately after an updated is flushed to the
DOM, but not on the initial render. It is passed the underlying DOM node, the
value, and any constant arguments passed to the render function.

`:on-render` - A function invoked immediately after the DOM is updated, both on
the initial render and subsequent updates. It is passed the underlying DOM node,
the value, the old value, and any constant arguments passed to the render
function.

`:on-unmount` - A function invoked immediately before the component is unmounted
from the DOM. It is passed the underlying DOM node, the most recent value and
the most recent constant args passed to the render fn.

`:will-appear` - A function invoked when this component is added to a mounting
container component. Invoked at the same time as :on-mount. It is passed the
underlying DOM node, a callback function, the most recent value and the most
recent constant args passed to the render fn. The callback should be called to
indicate that the element is done \"appearing\".

`:did-appear` - A function invoked immediately after the callback passed to
:will-appear is called. It is passed the underlying DOM node, the most recent
value, and the most recent constant args passed to the render fn.

`:will-enter` - A function invoked when this component is added to an already
mounted container component. Invoked at the same time as :on.mount. It is passed
the underlying DOM node, a callback function, the value and any constant args
passed to the render fn. The callback function should be called to indicate that
the element is done entering.

`:did-enter` - A function invoked after the callback passed to :will-enter is
called. It is passed the underlying DOM node, the value and any constant args
passed to the render fn.

`:will-leave` - A function invoked when this component is removed from its
containing component. Is passed the underlying DOM node, a callback function,
the most recent value and the most recent constant args passed to the render fn.
The DOM node will not be removed until the callback is called.

`:did-leave` - A function invoked after the callback passed to :will-leave is
called (at the same time as :on-unmount). Is passed the underlying DOM node, the
most recent value and the most recent constant args passed to the render fn.

### `(dumdom.core/defcomponent name & args)`

Creates a component with the given name, a docstring (optional), any number of
option->value pairs (optional), an argument vector and any number of forms body,
which will be used as the rendering function to dumdom.core/component.

For example:

```clj
(defcomponent Widget
  \"A Widget\"
  :on-mount #(...)
  :on-render #(...)
  [value constant-value]
  (some-child-components))
```

Is shorthand for:

```clj
(def Widget (dumdom.core/component
  (fn [value constant-value] (some-child-components))
  {:on-mount #(...)
  :on-render #(...)}))
```

### `(dumdom.core/TransitionGroup opt children)`

Exists solely for drop-in compatibility with Quiescent. Effectively does
nothing. Do not use for new applications.

### `(dumdom.core/CSSTransitionGroup opt children)`

Automates animation of entering and leaving elements via class names. If called
with `{:transitionName "example"}` as `opt`, child elements will have class
names set on them at appropriate times.

When the transition group mounts, all pre-existing children will have the class
name `example-enter` set on them. Then, `example-enter-active` is set. When all
transitions complete on the child node, `example-enter-active` will be removed
again.

When elements are added to an already mounted transition group, they will have
the class name `example-appear` added to them, _if appear animations are
enabled_ (they are not by default). Then the class name `example-appear-active`
will be set, and then removed after all transitions complete.

When elements are removed from the transition group, the class name
`example-leave` will be set, followed by `example-leave-active`, which is then
removed after transitions complete.

You can control which transitions are used on elements, and how their classes
are named with the following options:

#### `transitionName`

When set to a string: base-name for all classes. Can also be set to a map to
control individual class names:

```clj
{:transitionName {:enter "entrance"}} ;; entrance / entrance-active
{:transitionName {:enter "enter" :enterActive "entering"}} enter / entering
```

And similarly for `:leave`/`:leaveActive` and `:appear`/`:appearActive`.

#### `transitionEnter`

Boolean, set to `false` to disable enter transitions. Defaults to `true`.

#### `transitionAppear`

Boolean, set to `true` to enable appear transitions. Defaults to `false`.

#### `transitionLeave`

Boolean, set to `false` to disable leave transitions. Defaults to `true`.

### `(dumdom.dom/[el] attr children)`

Functions are defined for every HTML element:

```clj
(dumdom.dom/a {:href "https://cjohansen.no/"} "Blog")
```

Attributes are **not** optional, use an empty map if you don't have attributes.
Children can be text, components, virtual DOM elements (like the one above), or
a seq with a mix of those.

### `(dumdom.core/render component)`

Renders component to string. Available on Clojure as well, and can be used to do
server-side rendering of dumdom components.

### `(dumdom.core/inflate component el)`

Renders the component into the provided element. If `el` contains
server-rendered dumdom components, it will be inflated faster than a fresh
render (which forcefully rebuilds the entire DOM tree).

**NB!** Currently, only string keys are supported. If a component uses
non-string keys, inflating will not work, and it will be forcefully re-rendered.
This limitation might be adressed in a future release.

## Changelog

### 2018.12.xx (to be released)

- Added rendering components to strings
- Added inflating server-rendered DOM

### 2018.12.22

- Added snabbdom externs that hold up during advanced compilation

### 2018.12.21

Initial release

## License

Copyright © 2018 Christian Johansen

Distributed under the Eclipse Public License either version 1.0 or (at your
option) any later version.
