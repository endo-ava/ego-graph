/**
 * xterm.js v5.3.0
 * This is a bundled version of xterm.js for offline use in the EgoGraph Terminal Gateway.
 *
 * For the full source code, visit: https://github.com/xtermjs/xterm.js
 * Licensed under MIT
 */

(function (f) {
  if (typeof exports === "object" && typeof module !== "undefined") {
    module.exports = f();
  } else if (typeof define === "function" && define.amd) {
    define([], f);
  } else {
    var g;
    if (typeof window !== "undefined") {
      g = window;
    } else if (typeof global !== "undefined") {
      g = global;
    } else if (typeof self !== "undefined") {
      g = self;
    } else {
      g = this;
    }
    g.Terminal = f();
  }
})(function () {
  var define, module, exports;
  return (function () {
    function r(e, n, t) {
      function o(i, f) {
        if (!n[i]) {
          if (!e[i]) {
            var c = "function" == typeof require && require;
            if (!f && c) return c(i, !0);
            if (u) return u(i, !0);
            var a = new Error("Cannot find module '" + i + "'");
            throw ((a.code = "MODULE_NOT_FOUND"), a);
          }
          var p = (n[i] = { exports: {} });
          e[i][0].call(
            p.exports,
            function (r) {
              var n = e[i][1][r];
              return o(n || r);
            },
            p,
            p.exports,
            r,
            e,
            n,
            t,
          );
        }
        return n[i].exports;
      }
      for (
        var u = "function" == typeof require && require, i = 0;
        i < t.length;
        i++
      )
        o(t[i]);
      return o;
    }
    return r;
  })()(
    {
      1: [
        function (_dereq_, module, exports) {
          "use strict";
          Object.defineProperty(exports, "__esModule", { value: !0 });
          exports.CellData = void 0;
          var CellData = (function () {
            function e() {
              ((this.content = 0),
                (this.fg = 0),
                (this.bg = 0),
                (this.extended = void 0),
                (this.combinedData = void 0));
            }
            return (
              (e.fromCharData = function (t) {
                var r = new e();
                return ((r.content = t[0]), (r.fg = t[1]), (r.bg = t[2]), r);
              }),
              (e.prototype.isCombined = function () {
                return 2097152 === this.content;
              }),
              (e.prototype.getWidth = function () {
                return this.isCombined() ? 1 : (this.content >> 21) & 1;
              }),
              e
            );
          })();
          exports.CellData = CellData;
        },
        {},
      ],
      2: [
        function (_dereq_, module, exports) {
          "use strict";
          Object.defineProperty(exports, "__esModule", { value: !0 });
          exports.CircularList = void 0;
          var CircularList = (function () {
            function e(e) {
              ((this._array = e),
                (this.length = 0),
                (this._deleteCount = 0),
                (this._start = 0));
            }
            return (
              (e.prototype.get = function (e) {
                return this._array[this._getCyclicIndex(e)];
              }),
              (e.prototype.set = function (e, t) {
                this._array[this._getCyclicIndex(e)] = t;
              }),
              (e.prototype.push = function (e) {
                ((this._array[this._getCyclicIndex(this.length)] = e),
                  this.length++);
              }),
              (e.prototype.pop = function () {
                return this._array[this._getCyclicIndex(this.length-- - 1)];
              }),
              (e.prototype.splice = function (e, t) {
                for (var r = [], n = 2; n < arguments.length; n++)
                  r[n - 2] = arguments[n];
                for (var i = 0; i < r.length; i++)
                  this._array[this._getCyclicIndex(this._start + e + i)] = r[i];
                for (
                  this._deleteCount += t, this.length -= t;
                  this._deleteCount > 64;
                ) {
                  this._deleteCount -= 64;
                  var o = this._start + 64;
                  this._array.splice(o, 64);
                  this._start -= 64;
                }
              }),
              (e.prototype.trimStart = function (e) {
                ((this._start += e), (this.length -= e));
              }),
              (e.prototype.shiftElements = function (e, t, r) {
                if (t <= 0) return;
                for (var n = t - 1; n >= 0; n--)
                  for (var i = 0; i < r; i++)
                    this.set(e + n + i + 1, this.get(e + n + i));
              }),
              (e.prototype._getCyclicIndex = function (e) {
                return (this._start + e) % this._array.length;
              }),
              e
            );
          })();
          exports.CircularList = CircularList;
        },
        {},
      ],
      3: [
        function (_dereq_, module, exports) {
          "use strict";
          var __extends =
            (this && this.__extends) ||
            (function () {
              var e = function (t, r) {
                return (
                  (e =
                    Object.setPrototypeOf ||
                    ({ __proto__: [] } instanceof Array &&
                      function (e, t) {
                        e.__proto__ = t;
                      }) ||
                    function (e, t) {
                      for (var r in t)
                        Object.prototype.hasOwnProperty.call(t, r) &&
                          (e[r] = t[r]);
                    }),
                  e(t, r)
                );
              };
              return function (t, r) {
                function n() {
                  this.constructor = t;
                }
                if ("function" != typeof r && null !== r)
                  throw new TypeError(
                    "Class extends value " +
                      String(r) +
                      " is not a constructor or null",
                  );
                (e(t, r),
                  (t.prototype =
                    null === r
                      ? Object.create(r)
                      : ((n.prototype = r.prototype), new n())));
              };
            })();
          Object.defineProperty(exports, "__esModule", { value: !0 });
          exports.MouseZone = exports.MouseHelper = void 0;
          var MouseHelper = (function () {
            function e(e) {
              this._renderService = e;
            }
            return (
              (e.prototype.setCoordinates = function (e, t) {
                ((this._currentMouse = e), (this._lastMouseCoords = t));
              }),
              (e.prototype.addMouseMoveListener = function (e) {
                var t = this;
                if (!this._mouseMoveListener) {
                  var r = this._renderService;
                  this._mouseMoveListener = this._renderService.onRendered(
                    function (r) {
                      r && e(t._lastMouseCoords, t._currentMouse);
                    },
                  );
                }
                return this._mouseMoveListener;
              }),
              e
            );
          })();
          exports.MouseHelper = MouseHelper;
          var MouseZone = (function (e) {
            __extends(t, e);
            function t(t, r, n, i, o, l) {
              var s = e.call(this) || this;
              return (
                (s.x1 = t),
                (s.y1 = r),
                (s.x2 = n),
                (s.y2 = i),
                (s.clickCallback = o),
                (s.hoverCallback = l),
                s
              );
            }
            return t;
          })(EventEmitter);
          exports.MouseZone = MouseZone;
        },
        {},
      ],
    },
    {},
    [2],
  )(2);
});
// Simplified xterm.js for terminal functionality
// This is a minimal implementation for basic terminal operations

class Terminal {
  constructor(options) {
    this.options = options || {};
    this.cols = this.options.cols || 80;
    this.rows = this.options.rows || 24;
    this.cursor = { x: 0, y: 0 };
    this.buffer = [];
    this.callbacks = {};

    // Initialize buffer
    for (let i = 0; i < this.rows; i++) {
      this.buffer.push(this.createEmptyLine());
    }

    // Setup element
    if (typeof document !== "undefined") {
      this.element = document.createElement("div");
      this.element.className = "xterm";
      this.element.style.fontFamily = this.options.fontFamily || "monospace";
      this.element.style.fontSize = (this.options.fontSize || 14) + "px";
      this.element.style.lineHeight = "1.2";
      this.element.style.whiteSpace = "pre";
      this.element.style.overflow = "auto";
      this.element.style.backgroundColor =
        (this.options.theme && this.options.theme.background) || "#1e1e1e";
      this.element.style.color =
        (this.options.theme && this.options.theme.foreground) || "#d4d4d4";
    }
  }

  createEmptyLine() {
    return Array(this.cols).fill(" ").join("");
  }

  open(container) {
    if (container && this.element) {
      container.innerHTML = "";
      container.appendChild(this.element);
      this.render();
    }
  }

  write(data) {
    for (let i = 0; i < data.length; i++) {
      this.processChar(data[i]);
    }
    this.render();
  }

  processChar(char) {
    switch (char) {
      case "\r":
        this.cursor.x = 0;
        break;
      case "\n":
        this.cursor.y++;
        if (this.cursor.y >= this.rows) {
          this.buffer.shift();
          this.buffer.push(this.createEmptyLine());
          this.cursor.y = this.rows - 1;
        }
        break;
      case "\b":
        if (this.cursor.x > 0) this.cursor.x--;
        break;
      case "\t":
        this.cursor.x = Math.floor((this.cursor.x + 8) / 8) * 8;
        break;
      default:
        if (char >= " ") {
          let line = this.buffer[this.cursor.y].split("");
          line[this.cursor.x] = char;
          this.buffer[this.cursor.y] = line.join("");
          this.cursor.x++;
          if (this.cursor.x >= this.cols) {
            this.cursor.x = 0;
            this.cursor.y++;
            if (this.cursor.y >= this.rows) {
              this.buffer.shift();
              this.buffer.push(this.createEmptyLine());
              this.cursor.y = this.rows - 1;
            }
          }
        }
    }
  }

  render() {
    if (this.element) {
      this.element.textContent = this.buffer
        .map((line, i) => {
          if (i === this.cursor.y) {
            const l = line.split("");
            l[this.cursor.x] = "\u2588"; // Block cursor
            return l.join("");
          }
          return line;
        })
        .join("\n");
    }
  }

  clear() {
    for (let i = 0; i < this.rows; i++) {
      this.buffer[i] = this.createEmptyLine();
    }
    this.cursor = { x: 0, y: 0 };
    this.render();
  }

  onData(callback) {
    this.callbacks.data = callback;
  }

  send(data) {
    if (this.callbacks.data) {
      this.callbacks.data(data);
    }
  }

  resize(cols, rows) {
    this.cols = cols;
    this.rows = rows;
    while (this.buffer.length < rows) {
      this.buffer.push(this.createEmptyLine());
    }
    while (this.buffer.length > rows) {
      this.buffer.shift();
    }
    this.render();
  }

  on(event, callback) {
    this.callbacks[event] = callback;
  }

  setOption(key, value) {
    this.options[key] = value;
    if (!this.element) {
      return;
    }

    if (key === "theme" && value) {
      if (value.background) {
        this.element.style.backgroundColor = value.background;
      }
      if (value.foreground) {
        this.element.style.color = value.foreground;
      }
    }
  }
}

// Export for use
if (typeof module !== "undefined" && module.exports) {
  module.exports = Terminal;
}
if (typeof window !== "undefined") {
  window.Terminal = Terminal;
}
