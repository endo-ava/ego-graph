class Terminal {
  constructor(options) {
    this.options = options || {};
    this.cols = this.options.cols || 80;
    this.rows = this.options.rows || 24;
    this.cursor = { x: 0, y: 0 };
    this.buffer = [];
    this.callbacks = {};

    for (let i = 0; i < this.rows; i += 1) {
      this.buffer.push(this.createEmptyLine());
    }

    if (typeof document !== "undefined") {
      this.element = document.createElement("div");
      this.element.className = "xterm";
      this.element.style.fontFamily = this.options.fontFamily || "monospace";
      this.element.style.fontSize = (this.options.fontSize || 14) + "px";
      this.element.style.lineHeight = "1.2";
      this.element.style.whiteSpace = "pre";
      this.element.style.overflow = "auto";
      this.element.style.backgroundColor = "#1e1e1e";
      this.element.style.color = "#d4d4d4";
      this.element.style.webkitTextFillColor = "#d4d4d4";
      this.element.style.visibility = "visible";
      this.element.style.opacity = "1";
      this.element.style.position = "relative";
      this.element.style.zIndex = "1";
    }
  }

  createEmptyLine() {
    return Array(this.cols).fill(" ").join("");
  }

  open(container) {
    if (!container || !this.element) {
      return;
    }
    container.innerHTML = "";
    container.appendChild(this.element);
    this.render();
  }

  write(data) {
    var cleaned = data.replace(/\x1b\[[0-9;]*m/g, "");
    for (let i = 0; i < cleaned.length; i += 1) {
      this.processChar(cleaned[i]);
    }
    this.render();
  }

  processChar(char) {
    switch (char) {
      case "\r":
        this.cursor.x = 0;
        return;
      case "\n":
        this.cursor.y += 1;
        if (this.cursor.y >= this.rows) {
          this.buffer.shift();
          this.buffer.push(this.createEmptyLine());
          this.cursor.y = this.rows - 1;
        }
        return;
      case "\b":
        if (this.cursor.x > 0) {
          this.cursor.x -= 1;
        }
        return;
      case "\t":
        this.cursor.x = Math.floor((this.cursor.x + 8) / 8) * 8;
        return;
      default:
        break;
    }

    if (char < " ") {
      return;
    }

    if (this.cursor.x < 0) {
      this.cursor.x = 0;
    }
    if (this.cursor.x >= this.cols) {
      this.cursor.x = 0;
      this.cursor.y += 1;
    }
    if (this.cursor.y >= this.rows) {
      this.buffer.shift();
      this.buffer.push(this.createEmptyLine());
      this.cursor.y = this.rows - 1;
    }

    const line = this.buffer[this.cursor.y].split("");
    line[this.cursor.x] = char;
    this.buffer[this.cursor.y] = line.join("");

    this.cursor.x += 1;
    if (this.cursor.x >= this.cols) {
      this.cursor.x = 0;
      this.cursor.y += 1;
      if (this.cursor.y >= this.rows) {
        this.buffer.shift();
        this.buffer.push(this.createEmptyLine());
        this.cursor.y = this.rows - 1;
      }
    }
  }

  render() {
    if (!this.element) {
      return;
    }

    this.element.textContent = this.buffer
      .map((line, idx) => {
        if (idx !== this.cursor.y) {
          return line;
        }
        const chars = line.split("");
        if (this.cursor.x >= 0 && this.cursor.x < chars.length) {
          chars[this.cursor.x] = "\u2588";
        }
        return chars.join("");
      })
      .join("\n");
  }

  clear() {
    for (let i = 0; i < this.rows; i += 1) {
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
  }
}

if (typeof module !== "undefined" && module.exports) {
  module.exports = Terminal;
}
if (typeof window !== "undefined") {
  window.Terminal = Terminal;
}
