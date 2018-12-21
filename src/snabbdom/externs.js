var vdomNode = {
  /** @type {Array<vdomNode>} */
  children: [],
  data: {
    attrs: {},
    hook: {
      destroy: function () {},
      update: function () {},
      insert: function () {}
    }
  },
  elm: {},
  key: {},
  listener: function () {},
  sel: "",
  text: ""
};

var snabbdom = {
  /**
   * @return {vdomNode}
   */
  h: function () {},
  init: function () {},
  thunk: function () {},
  props: {
    create: function () {},
    update: function () {}
  },
  attributes: {
    create: function () {},
    update: function () {}
  },
  eventlisteners: {
    create: function () {},
    update: function () {},
    destroy: function () {}
  },
  style: {
    create: function () {},
    update: function () {},
    destroy: function () {},
    remove: function () {}
  }
};
