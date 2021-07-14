# dumdom - The dumb DOM component library

**dumdom** is a component library that renders (and re-renders) immutable data
efficiently. It delivers on the basic value proposition of React and its peers
while eschewing features like component local state and object oriented APIs,
and embracing ClojureScript features like immutable data structures.

**dumdom** is API compatible with
[Quiescent](https://github.com/levand/quiescent/), and can be used as a drop-in
replacement for it so long as you don't use React features directly. Refer to
[differences from React](#differences-from-react) for things to be aware of.

**dumdom** is currently a wrapper for
[Snabbdom](https://github.com/snabbdom/snabbdom), but that should be considered
an implementation detail, and may be subject to change. Using snabbdom features
not explicitly exposed by dumdom is **not** recommended.

**dumdom** aims to be finished, stable, and worthy of your trust. Breaking
changes will never be intentionally introduced to the codebase. For this reason,
dumdom does not adhere to the "semantic" versioning scheme.

In addition to being API compatible with Quiescent, **dumdom** supports:

- Rendering to strings (useful for server-side rendering from both the JVM and node.js)
- Efficient "inflation" of server-rendered markup on the client side
- Hiccup syntax for components

## Table of contents

* [Install](#install)
* [Example](#example)
* [Rationale](#rationale)
* [Limitations](#limitations)
* [Differences from Quiescent](#differences-from-quiescent)
* [Using with Devcards](#using-with-devcards)
* [Contribute](#contribute)
* [Documentation](#documentation)
  * [Building virtual DOM](#building-virtual-dom)
  * [Event listeners](#event-listeners)
  * [Creating components](#creating-components)
  * [CSS transitions](#css-transitions)
  * [Class name transitions](#class-name-transitions)
  * [Refs](#refs)
  * [Server-rendering](#server-rendering)
  * [API Docs](#api-docs)
* [Examples](#examples)
* [Changelog](#changelog)
* [Roadmap](#roadmap)
* [License](#license)

## Install

With tools.deps:

```clj
cjohansen/dumdom {:mvn/version "2021.06.29"}
```

With Leiningen:

```clj
[cjohansen/dumdom "2021.06.29"]
```

## Example

Using hiccup-style data:

```clj
(require '[dumdom.core :as dumdom :refer [defcomponent]])

(defcomponent heading
  :on-render (fn [dom-node val old-val])
  [data]
  [:h2 {:style {:background "#000"}} (:text data)])

(defcomponent page [data]
  [:div
    [heading (:heading data)]
    [:p (:body data)]])

(dumdom/render
 [page {:heading {:text "Hello world"}
        :body "This is a web page"}]
 (js/document.getElementById "app"))
```

Using the Quiescent-compatible function API:

```clj
(require '[dumdom.core :as dumdom :refer [defcomponent]]
         '[dumdom.dom :as d])

(defcomponent heading
  :on-render (fn [dom-node val old-val])
  [data]
  (d/h2 {:style {:background "#000"}} (:text data)))

(defcomponent page [data]
  (d/div {}
    (heading (:heading data))
    (d/p {} (:body data))))

(dumdom/render
 (page {:heading {:text "Hello world"}
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

## Differences from React

In React, [`onChange` is really
`onInput`](https://github.com/facebook/react/issues/9567). This is not true in
dumdom. When swapping out Quiescent and React for dumdom, you must replace
all occurrences of `onChange` with `onInput` to retain behavior.

## Using with Devcards

[Devcards](https://github.com/bhauman/devcards) is a system for rendering React
components in isolation. Because **dumdom** components are not React components,
they need some wrapping for Devcards to make sense of them.

You need to add [dumdom-devcards](https://github.com/cjohansen/dumdom-devcards)
as a separate dependency. Then use the `dumdom.devcards` namespace just like you
would `devcards.core`:

```clj
(require '[dumdom.devcards :refer-macros [defcard]])

(defcard my-dumdom-card
  (my-dumdom-component {:value 0}))
```

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
clojure -A:dev:repl
```

Then open [http://localhost:9595/figwheel-extra-main/tests](http://localhost:9595/figwheel-extra-main/tests).

If you're not yet sure how to formulate a test for your feature, fire up
[http://localhost:9595/](http://localhost:9595/) and play around in
[./dev/dumdom/dev.cljs](./dev/dumdom/dev.cljs) until you figure it out. More
visually oriented code can be tested with devcards instead. Add a devcard to
[./devcards/dumdom](./devcards/dumdom), and inspect the results at
[http://localhost:9595/devcards.html](http://localhost:9595/devcards.html)

If you have ideas for new features, please open an issue to discuss the idea and
the API before implementing it to avoid putting lots of work into a pull request
that might be rejected. I intend to keep **dumdom** a focused package, and don't
want it to accrete a too wide/too losely coherent set of features.

### Running from Emacs

There is a `.dir-locals.el` file in the root of this repo to help you out. Run
`cider-jack-in-cljs`, and you should get a REPL and figwheel running on port 9595:

- [Dev scratchpad](http://localhost:9595/)
- [Devcards](http://localhost:9595/devcards.html)
- [Tests](http://localhost:9595/figwheel-extra-main/tests)

## Documentation

The vast majority of use-cases are covered by using [hiccup-style markup]() for
DOM elements, defining custom components with [`defcomponent`](#defcomponent),
and rendering the resulting virtual DOM to an element with [`render`](#render):

```clj
(require '[dumdom.core :as dumdom :refer [defcomponent]])

(defcomponent my-component [data]
  [:div
    [:h1 "Hello world!"]
    [:p (:message data)]])

(dumdom/render
  (my-component {:message "Hello, indeed"})
  (js/document.getElementById "app"))
```

Components defined by `defcomponent` are functions, as demonstrated in the above
example. You can also use them for hiccup markup, e.g.:

```clj
(dumdom/render
  [my-component {:message "Hello, indeed"}]
  (js/document.getElementById "app"))
```

The strength of hiccup markup is being able to represent DOM structures as pure
data. Because functions are not data, there is no real benefit to using hiccup
syntax for custom components, so I typically don't, but it doesn't make any
difference either way.

### Building virtual DOM

Virtual DOM elements are built with hiccup markup:

```clj
[tagname attr? children...]
```

`tagname` is always a keyword, attributes are in an optional map, and there
might be one or more children, or a list of children. Beware that children
should not be provided as a vector, lest it be interpreted as a new hiccup
element.

**Note:** dumdom currently does not support inlining class names and ids on the
tag name selector (e.g. `:div.someclass#someid`). This might be added in a
future release.

For API compatibility with Quiescent, elements can also be created with the
functions in `dumdom.dom`:

```clj
(dumdom.dom/div {:style {:border "1px solid red"}} "Hello world")
```

Note that with these functions, the attribute map is not optional, and must
always be provided, even if empty.

#### Keys

You can specify the special attribute `:key` do help dumdom recognize DOM
elements that move. `:key` should be set to a value that is unique among the
element's siblings. For instance, if you are rendering lists of things, setting
a key on each item means dumdom can update the rendered view by simply moving
existing elements around in the DOM. Not setting the key will lead dumdom to
work harder to align the DOM with the virtual representation:

```clj
(require '[dumdom.core :as dumdom :refer [defcomponent]])

(defcomponent list-item [fruit]
  [:li {:key fruit} fruit])

(def el (js/document.getElementById "app"))

(dumdom/render [:ul (map list-item ["Apples" "Oranges" "Kiwis"])] el)

;; This will now result in reordering the DOM elements, instead of recreating them
(dumdom/render [:ul (map list-item ["Oranges" "Apples" "Kiwis"])] el)
```

### Event listeners

To attach events to your virtual DOM nodes, provide functions to camel-cased
event name keys in the attribute map:

```clj
[:a {:href "#"
     :onClick (fn [e]
                (.preventDefault e)
                (prn "You clicked me!"))} "Click me!"]
```

### Creating components

You create components with `defcomponent` or `component` - the first is
just a convenience macro for `def` + `component`:

```clj
(require '[dumdom.core :refer [component defcomponent]])

(defcomponent my-component
  :on-render (fn [e] (js/console.log "Rendered" e))
  [data]
  [:div "Hello world"])

;; ...is the same as:

(def my-component
  (component
    (fn [data]
      [:div "Hello world"])
    {:on-render (fn [e] (js/console.log "Rendered" e))}))
```

Refer to the API docs for [`component`](#component) for details on what options
it supports, life-cycle hooks etc, and the API docs for
[`defcomponent`](#defcomponent) for more on how to use it.

A dumdom component is a function. When you call it with data it returns
something that dumdom knows how to render, e.g.:

```clj
(dumdom.core/render (my-component {:id 42}) root-el)
```

You can also invoke the component with hiccup markup, although there is no real
benefit to doing so - the result is exactly the same:

```clj
(dumdom.core/render [my-component {:id 42}] root-el)
```

#### Component arguments

When you call a dumdom component with data, it will recreate the virtual DOM
node only if the data has changed since it was last called. However, this
decision is based solely on the first argument passed to the component. So while
you can pass any number of arguments to a component beware that only the first
one is used to influence rendering decisions.

This design is inherited from Quiescent, and the idea is that you can pass along
things like core.async message channels without having them interferring with
the rendering decisions. When passing more than one argument to a dumdom
component, make sure that any except the first one are constant for the lifetime
of the component.

This only applies to components created with `component`/`defcomponent`, not
virtual DOM functions, which take any number of DOM children.

### CSS transitions

CSS transitions can be defined inline on components to animate the appearing or
disappearing of elements. There are three keys you can use to achieve this
effect:

- `:mounted-style` - Styles that will apply after the element has been mounted
- `:leaving-style` - Styles that will apply before the element is removed from
  its parent - the element will not be removed until all its transitions
  complete
- `:disappearing-style` - Styles that will apply before the element is removed
  along with its parent element is being removed - the element will not be
  removed until all its transitions are complete

As an example, if you want an element to fade in, set its opacity to 0, and then
its `:mounted-style` opacity to 1. To fade it out as well, set its
`:leaving-styles` opacity to 0 again. Remember to enable transitions for the
relevant CSS property:

```clj
[:div {:style {:opacity "0"
               :transition "opacity 0.25s"}
       :mounted-style {:opacity "1"}
       :leaving-style {:opacity "0"}}
  "I will fade both in and out"]
```

### Class name transitions

In order to be API compatible with Quiescent, dumdom supports React's
`CSSTransitionGroup` for doing enter/leave transitions with class names instead
of inline CSS. Given the following CSS:

```css
.example-leave {
  opacity: 1;
  transition: opacity 0.25s;
}

.example-leave-active {
  opacity: 0;
}
```

Then we could fade out an element with:

```clj
(require '[dumdom.core :refer [CSSTransitionGroup]])

(CSSTransitionGroup {:transitionName "example"}
  [[:div "I will fade out"]])
```

Note that `CSSTransitionGroup` takes a vector/seq of children. Refer to the
[API docs for `CSSTransitionGroup`](#css-transition-group) for more details. In
general, using inline CSS transitions will be more straight-forward, and is
recommended.

### Refs

A `:ref` on an element is like an `:on-mount` callback that you can attach from
"the outside":

```clj
;; NB! Just an example, there are better ways to do this with CSS

(defn square-element [el]
  (set! (.. el -style -height) (str (.-offsetWidth el) "px")))

[:div {:style {:border "1px solid red"}
       :ref square-element} "I will be in a square box"]
```

The `:ref` function will be called only once, when the element is first mounted.
Use this feature with care - do not use it with functions that behave
differently at different times. Consider this example:

```clj
(defcomponent my-component [data]
  [:div
    [:h1 "Example"]
    [:div {:ref (when (:actionable? data)
                  setup-click-indicator)}
      "I might or might not be clickable"]])
```

While this looks reasonable, refs are only called when the element mounts. Thus,
if the value of `(:actionable? data)` changes, the changes will not be reflected
on the element. If you need to conditionally make changes to an element this
way, create a custom component and use the `:on-render` hook instead, which is
called every time data changes.

### Server rendering

Dumdom supports rendering your components to strings on the server and then
"inflating" the view client-side. Inflating consists of associating the
resulting DOM elements with their respective virtual DOM nodes, so dumdom can
efficiently update your UI, and adding client-side event handlers so users can
interact with your app.

Even though it sounds straight-forward, using server rendering requires that
you write your entire UI layer in a way that can be loaded on both the server
and client. This is easier said than done.

To render your UI to a string on the server:

```clj
(require '[dumdom.string :as dumdom])

(defn body []
  (str "<html><body><div id=\"app\">"
       (dumdom/render [:div [:h1 "Hello world"]])
       "</div></body></html>"))

(defn index [req]
  {:status 200
   :headers {"content-type" "text/html"}
   :body (body)})
```

Then, on the client:

```clj
(require '[dumdom.inflate :as dumdom])

(dumdom/render
  [:div [:h1 "Hello world]]
  (js/document.getElementById "app"))
```

To update your view, either call `dumdom.inflate/render` again, or use
`dumdom.core/render`.

### API Docs

<a id="render"></a>
#### `(dumdom.core/render component element)`

Render the virtual DOM node created by the component into the specified DOM
element. Component can be either hiccup-style data, like `[:div {} "Hello"]` or
the result of calling component functions, e.g. `(dumdom.dom/div {} "Hello")`.

<a id="unmount"></a>
#### `(dumdom.core/unmounet element)`

Clear the element and discard any internal state related to it.

<a id="render-once"></a>
#### `(dumdom.core/render-once component element)`

Like `dumdom.core/render`, but entirely stateless. `render` needs to use memory
in order for subsequent calls to render as little as possible as fast as
possible. If you don't intend to update the rendered DOM structure,
`render-once` is more efficient as it does not use any memory. Subsequent calls
to this function with the same arguments will always destructively re-render the
entire tree represented by the component.

<a id="component"></a>
#### `(dumdom.core/component render-fn [opt])`

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

The rendering function can return hiccup-style data or the result of calling
component functions.

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

<a id="defcomponent"></a>
#### `(dumdom.core/defcomponent name & args)`

Creates a component with the given name, a docstring (optional), any number of
option->value pairs (optional), an argument vector and any number of forms body,
which will be used as the rendering function to dumdom.core/component.

For example:

```clj
(defcomponent widget
  \"A Widget\"
  :on-mount #(...)
  :on-render #(...)
  [value constant-value]
  (some-child-components))
```

Is shorthand for:

```clj
(def widget (dumdom.core/component
  (fn [value constant-value] (some-child-components))
  {:on-mount #(...)
   :on-render #(...)}))
```

#### `(dumdom.core/TransitionGroup opt children)`

Exists solely for drop-in compatibility with Quiescent. Effectively does
nothing. Do not use for new applications.

<a id="css-transition-group"></a>
#### `(dumdom.core/CSSTransitionGroup opt children)`

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

##### `transitionName`

When set to a string: base-name for all classes. Can also be set to a map to
control individual class names:

```clj
{:transitionName {:enter "entrance"}} ;; entrance / entrance-active
{:transitionName {:enter "enter" :enterActive "entering"}} enter / entering
```

And similarly for `:leave`/`:leaveActive` and `:appear`/`:appearActive`.

##### `transitionEnter`

Boolean, set to `false` to disable enter transitions. Defaults to `true`.

##### `transitionAppear`

Boolean, set to `true` to enable appear transitions. Defaults to `false`.

##### `transitionLeave`

Boolean, set to `false` to disable leave transitions. Defaults to `true`.

#### `(dumdom.dom/[el] attr children)`

Functions are defined for every HTML element:

```clj
(dumdom.dom/a {:href "https://cjohansen.no/"} "Blog")
```

Attributes are **not** optional, use an empty map if you don't have attributes.
Children can be text, components, virtual DOM elements (like the one above), or
a seq with a mix of those.

#### `(dumdom.core/render-string component)`

Renders component to string. Available on Clojure as well, and can be used to do
server-side rendering of dumdom components.

#### `(dumdom.inflate/render component el)`

Renders the component into the provided element. If `el` contains
server-rendered dumdom components, it will be inflated faster than a fresh
render (which forcefully rebuilds the entire DOM tree).

**NB!** Currently, only string keys are supported. If a component uses
non-string keys, inflating will not work, and it will be forcefully re-rendered.
This limitation might be adressed in a future release.

<a id="render-eagerly"></a>
#### `dumdom.component/*render-eagerly?*`

When this var is set to `true`, every existing component will re-render on the
next call after a new component has been created, even if the input data has not
changed. This can be useful in development - if you have any level of
indirection in your rendering code (e.g. passing a component function as the
"static arg" to another component, multi-methods, etc), you are not guaranteed
to have all changed components re-render after a compile and hot swap. With this
var set to `true`, changing any code that defines a dumdom component will cause
all components to re-render.

The var defaults to `false`, in which case it has no effect. Somewhere in your
development setup, add

```clj
(set! dumdom.component/*render-eagerly?* true)
```

## Examples

Unfortunately, there is no TodoMVC implementation yet, but there is
[Yahtzee](https://github.com/cjohansen/yahtzee-cljs/)! Please get in touch if you've
used dumdom for anything and I'll happily include a link to your app.

Check out this cool [dungeon crawler](http://heck.8620.cx/)
([source](https://github.com/uosl/heckendorf)) made with dumdom.

## Changelog

### 2021.07.08

- Enumerate keys, so duplicated keys on the same level in the virtual DOM tree
  do not cause elements or components to get mixed up with each other. This
  fixes a bug where two components sharing key *and* data would share a
  cache-entry, and only the first instance would have its life-cycle hooks
  called.

### 2021.06.29

- Bugfix: When a component rendered another component directly (e.g. with no
  additional wrapping DOM elements), Dumdom would not call the inner component's
  `:on-unmount` hook - now it does.
- Force elements with innerHTML to have a key. Works around shortcomings in
  Snabbdom related to innerHTML manipulation.
- New featyre: `dumdom.core/unmount` (see [docs above](#unmount)).
- New feature: `dumdom.core/render-once` (see [docs above](#render-once)).
- Major implementation change: Move Dumdom's vdom representation from Snabbdom's
  object model to Clojure maps. This moves more of the implementation from
  JavaScript to Clojure, and more importantly addresses som weird behavior in
  complex DOM layouts. The vdom objects created by Snabbdom are mutated by
  Snabbdom to maintain a reference to the rendered elements. In other words, the
  vdom objects we provide as input to Snabbdom ends up doubling as Snabbdom's
  internal state. Hanging on to these from the outside and feeding them back to
  Snabbdom at a later point (e.g. when `should-component-update?` is `false`)
  _mostly_ works, but has been proven to cause very unfortunate behavior in
  certain bespoke situations.

  As always, there are no changes to the public API. However, this is a invasive
  change to Dumdom's implementation, so rigorous testing is adviced.

  **NB!** The internal data-structure of virtual DOM nodes have changed as a
  consequence, both for Clojure (different map structure) and ClojureScript
  (maps, not Snabbdom objects). This is considered implementation changes, as
  these are not documented features of Dumdom. If you have somehow ended up
  relying on those you should investigate changes further.

### 2021.06.21

- Render comment nodes in place of `nil`s. This works around a quirk of Snabbdom
  (as compared to React) where replacing a `nil` with an element can prematurely
  cause transition effects due to how Snabbdom reuses DOM elements. See [this
  issue](https://github.com/snabbdom/snabbdom/issues/973) for more information.

### 2021.06.18

- Retracted due to a typo which made the artefact unusable. Sorry about that!

### 2021.06.16

- BREAKING: Dumdom no longer bundles `dumdom.devcards` - add
  [dumdom-devcards](https://github.com/cjohansen/dumdom-devcards) separately to
  your dependencies. This change only affects projects using `dumdom.devcards`,
  and requires no other changes to your code than adding the separate namespace.
  I'm very sorry for this breaking change, but including devcards was a mistake
  that required you to have devcards on path in production, which is not a good
  place to be.
- Bug fix: `{:dangerouslySetInnerHTML {:__html nil}}` was mistakenly a noop. It
  now clears any previously set `innerHTML`.
- Work around [a bug in Snabbdom](https://github.com/snabbdom/snabbdom/issues/970)
  by temporarily bundling a patched version.

### 2021.06.11

- Properly support data-attributes. Just include them with the data- prefix, and
  dumdom will render them: `[:div {:data-id "123"} "Hello"]`.

### 2021.06.10

- Upgrade Snabbdom to version 3.0.3
- Make sure all style properties are strings. Fixes a strange glitch where
  `:opacity 0` would not always set opacity to 0.

### 2021.06.08

- Pixelize more styles, switch from a allow-list to a deny-list of properties to
  pixelize. Thanks to [Magnar Sveen](https://github.com/magnars).

### 2021.06.07

- Add feature to eagerly re-render components in development, see
  `*render-eagerly?*` above.

- Pass old data to `:on-render` and `:on-update` functions. Previously, these
  would receive `[dom-el data statics]`, now they fully match Quiescent's
  behavior, receiving `[dom-el data old-data statics]`.

### 2021.06.02

- Allow `TransitionGroup` to take either a single component or a seq of
  components.

### 2021.06.01

- Bugfix: Don't throw exceptions when components return nil
- If the root component returns nil, remove previously rendered content.

### 2021.05.31

- Support pixel values for `:border`, `:border-left`, `:border-right`,
  `:border-top`, `:border-bottom`.

### 2021.05.28

- Bug fix: Using non-primitive values (including keywords) with component keys
  (both inline `:key` and the result from `:keyfn`) would cause weird rendering
  issues. Any value can now be safely used as a component key.
- Bug fix: CSS properties that take pixel values could only be specified as
  numbers when the camelCased property name was used. Now also supports this
  behavior with lisp-casing, e.g. `:style {:margin-left 10}` produces a 10 pixel
  left margin.
- Bug fix: Support numbers for `border-{bottom,top}-{left,right}-radius` (and
  their camel cased counterparts).
- Remove an attempt at a micro-optimization that instead caused a minimal
  performance penalty.

### 2021.05.07

- Fix failing production builds of 2020.10.19 due to missing externs

### 2020.10.19

- Add support for Shadow CLJS

### 2020.07.04

- Properly render nested seqs to DOM strings

### 2020.06.21

- Don't render `:ref` functions to the DOM
- Don't render `nil` styles when rendering to strings

### 2020.06.04

- Added support for styles as strings in hiccup elements, e.g.
  `[:a {:style "padding: 2px"}]`

### 2020.02.12

- Bugfix: Don't trip on numbers when rendering to string
- Bugfix: Don't trip on event handlers when rendering to string

### 2020.01.27

- Added support for `:dangerouslySetInnerHTML`

### 2019.09.16

- Fixed animation style properties, which where inadvertently broken in the
  previous release :'(

### 2019.09.05-1

- Support using dashed cased attributes, e.g. `:xlink-href`, `:view-box` etc
- When passing `nil` to an attribute, do not render that attribute with the
  string "null" or an empty value - remove the attribute

### 2019.09.05

- Support `:div.class#id` style hiccup in server-rendering as well.

### 2019.02.03-3

- Built jar with a different version of `pack.alpha`, so
  [cljdoc](https://cljdoc.org) is able to analyze it

### 2019.01.21

- Document and launch `:mounted-style`, `:leaving-style`, and `:disappearing-style`

### 2019.01.19

- Added support for hiccup-style data
- Added rendering components to strings
- Added inflating server-rendered DOM

### 2018.12.22

- Added snabbdom externs that hold up during advanced compilation

### 2018.12.21

Initial release

## Roadmap

- Provide TodoMVC app
- Port Snabbdom (roughly, not API compatibly) to ClojureScript

## License

Copyright © 2018-2021 Christian Johansen

Distributed under the Eclipse Public License either version 1.0 or (at your
option) any later version.
