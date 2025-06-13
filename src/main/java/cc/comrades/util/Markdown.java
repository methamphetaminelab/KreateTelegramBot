package cc.comrades.util;

import cc.comrades.model.entity.TelegramSession;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Markdown {
    private interface Node {
        String render();
    }

    private final List<Node> nodes = new ArrayList<>();

    private Markdown() {}

    public static Markdown create() {
        return new Markdown();
    }

    public static Markdown create(String text) {
        return Markdown.create().text(text);
    }

    public Markdown text(String text) {
        if (text == null || text.isBlank()) return this;
        nodes.add(new TextNode(text));
        return this;
    }

    public Markdown newLine() {
        nodes.add(new TextNode("  \n"));
        return this;
    }

    public Markdown bold(String text) {
        if (text == null || text.isBlank()) return this;
        nodes.add(new BoldNode(new TextNode(text)));
        return this;
    }

    public Markdown italic(String text) {
        if (text == null || text.isBlank()) return this;
        nodes.add(new ItalicNode(new TextNode(text)));
        return this;
    }

    public Markdown underline(String text) {
        if (text == null || text.isBlank()) return this;
        nodes.add(new UnderlineNode(new TextNode(text)));
        return this;
    }

    public Markdown strikethrough(String text) {
        if (text == null || text.isBlank()) return this;
        nodes.add(new StrikethroughNode(new TextNode(text)));
        return this;
    }

    public Markdown spoiler(String text) {
        if (text == null || text.isBlank()) return this;
        nodes.add(new SpoilerNode(new TextNode(text)));
        return this;
    }

    public Markdown inlineCode(String text) {
        if (text == null || text.isBlank()) return this;
        nodes.add(new InlineCodeNode(new TextNode(text)));
        return this;
    }

    public Markdown codeBlock(String text) {
        if (text == null || text.isBlank()) return this;
        nodes.add(new CodeBlockNode(text));
        return this;
    }

    public Markdown codeBlock(String text, String language) {
        if (text == null || text.isBlank()) return this;
        nodes.add(new CodeBlockNode(language, text));
        return this;
    }

    public Markdown link(String label, String url) {
        if (label == null || label.isBlank() || url == null || url.isBlank()) return this;
        nodes.add(new LinkNode(label, url));
        return this;
    }

    public Markdown append(Markdown other) {
        nodes.addAll(other.nodes);
        return this;
    }

    public Markdown mention(TelegramSession session, String label) {
        return mention(session.getTelegramUsername(), label, session.getChatId());
    }

    public Markdown mention(String label, long userId) {
        return mention(null, label, userId);
    }

    public Markdown mention(String username, String label, long userId) {
        if (label == null || label.isBlank()) return this;
        Node node;
        if (username != null && !username.isEmpty()) {
            node = new TextNode('@' + username);
        } else {
            node = new LinkNode(label, "tg://user?id=" + userId);
        }
        nodes.add(node);
        return this;
    }

    public String build() {
        return toString();
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (Node node : nodes) {
            stringBuilder.append(node.render());
        }
        return stringBuilder.toString();
    }

    private static String escape(String text) {
        if (text == null) return "";
        return text
                .replace("\\", "\\\\")
                .replace("_", "\\_")
                .replace("*", "\\*")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("~", "\\~")
                .replace("`", "\\`")
                .replace(">", "\\>")
                .replace("#", "\\#")
                .replace("+", "\\+")
                .replace("-", "\\-")
                .replace("=", "\\=")
                .replace("|", "\\|")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace(".", "\\.")
                .replace("!", "\\!");
    }

    private record TextNode(String text) implements Node {
        public String render() {
            return escape(text);
        }
    }

    private static abstract class WrapperNode implements Node {
        private final List<Node> children;

        private WrapperNode(Node... children) {
            this.children = Arrays.asList(children);
        }

        protected String inner() {
            StringBuilder stringBuilder = new StringBuilder();
            for (Node child : children) {
                stringBuilder.append(child.render());
            }
            return stringBuilder.toString();
        }
    }

    private static final class BoldNode extends WrapperNode {
        private BoldNode(Node... children) {
            super(children);
        }

        public String render() {
            return "*" + inner() + "*";
        }
    }

    private static final class ItalicNode extends WrapperNode {
        private ItalicNode(Node... children) {
            super(children);
        }

        public String render() {
            return "_" + inner() + "_";
        }
    }

    private static final class UnderlineNode extends WrapperNode {
        private UnderlineNode(Node... children) {
            super(children);
        }

        public String render() {
            return "__" + inner() + "__";
        }
    }

    private static final class StrikethroughNode extends WrapperNode {
        private StrikethroughNode(Node... children) {
            super(children);
        }

        public String render() {
            return "~" + inner() + "~";
        }
    }

    private static final class SpoilerNode extends WrapperNode {
        private SpoilerNode(Node... children) {
            super(children);
        }

        public String render() {
            return "||" + inner() + "||";
        }
    }

    private static final class InlineCodeNode extends WrapperNode {
        private InlineCodeNode(Node... children) {
            super(children);
        }

        public String render() {
            return "`" + inner() + "`";
        }
    }

    private static final class CodeBlockNode implements Node {
        private final String language;
        private final String content;

        private CodeBlockNode(String content) {
            this.language = null;
            this.content = content;
        }

        private CodeBlockNode(String language, String content) {
            this.language = language;
            this.content = content;
        }

        public String render() {
            if (language == null) {
                return "```" + escape(content) + "```";
            } else {
                return "```" + language + "\n" + content + "\n```";
            }
        }
    }

    private record LinkNode(String label, String url) implements Node {

        public String render() {
            return "[" + escape(label) + "](" + escape(url) + ")";
        }
    }
}
