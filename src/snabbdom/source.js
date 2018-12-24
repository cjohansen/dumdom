import vnode from '../../snabbdom/es/vnode';
import htmlDomApi from '../../snabbdom/es/htmldomapi';

const snabbdom = require('../../snabbdom/es/snabbdom');
snabbdom.attributes = require('../../snabbdom/es/modules/attributes').default;
snabbdom.props = require('../../snabbdom/es/modules/props').default;
snabbdom.style = require('../../snabbdom/es/modules/style').default;
snabbdom.eventlisteners = require('../../snabbdom/es/modules/eventlisteners').default;
module.exports = snabbdom;

snabbdom.tovnode = function toVNode(node, domApi) {
  var api = domApi !== undefined ? domApi : htmlDomApi;
  var text;

  if (api.isElement(node)) {
    var id = node.id ? '#' + node.id : '';
    var cn = node.getAttribute('class');
    var c = cn ? '.' + cn.split(' ').join('.') : '';
    var sel = api.tagName(node).toLowerCase() + id + c;
    var props = {attrs: {}};
    var children = [];
    var elmAttrs = node.attributes;
    var elmChildren = node.childNodes;

    for (var i = 0, n = elmAttrs.length; i < n; i++) {
      var attrName = elmAttrs[i].nodeName;

      if (attrName === 'data-dumdom-key') {
        props.key = elmAttrs[i].nodeValue;
      } else if (attrName !== 'id' && attrName !== 'class') {
        props.attrs[attrName] = elmAttrs[i].nodeValue;
      }
    }

    for (i = 0, n = elmChildren.length; i < n; i++) {
      children.push(toVNode(elmChildren[i], domApi));
    }

    return vnode(sel, props, children, undefined, node);
  } else if (api.isText(node)) {
    text = api.getTextContent(node);
    return vnode(undefined, undefined, undefined, text, node);
  } else if (api.isComment(node)) {
    text = api.getTextContent(node);
    return vnode('!', {}, [], text, node);
  } else {
    return vnode('', {}, [], undefined, node);
  }
};
