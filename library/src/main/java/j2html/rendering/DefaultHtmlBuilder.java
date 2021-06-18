package j2html.rendering;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;

import j2html.Config;
import j2html.utils.TextEscaper;

public class DefaultHtmlBuilder<A extends Appendable> implements HtmlBuilder<A> {

    private final A out;
    private TextEscaper textEscaper;
    private boolean emptyTagsClosed;
    private String indent;
    private HtmlBuilder<A> delegate;

    DefaultHtmlBuilder(final A out) {
        this.out = out;
        this.textEscaper = Config.global().textEscaper();
        this.emptyTagsClosed = Config.global().closeEmptyTags();
        setDelegate();
    }

    private void setDelegate() {
        this.delegate = indent != null ? new IndentedHtml(indent, emptyTagsClosed) : new FlatHtml(emptyTagsClosed);
    }

    public DefaultHtmlBuilder<A> withIndent(final String indent) {
        this.indent = indent;
        setDelegate();
        return this;
    }

    public DefaultHtmlBuilder<A> withEmptyTagsClosed(final boolean emptyTagsClosed) {
        this.emptyTagsClosed = emptyTagsClosed;
        setDelegate();
        return this;
    }

    public DefaultHtmlBuilder<A> withTextEscaper(final TextEscaper textEscaper) {
        this.textEscaper = textEscaper;
        return this;
    }

    @Override
    public TagBuilder appendStartTag(final String name) throws IOException {
        return delegate.appendStartTag(name);
    }

    @Override
    public HtmlBuilder<A> appendEndTag(final String name) throws IOException {
        delegate.appendEndTag(name);
        return this;
    }

    @Override
    public TagBuilder appendEmptyTag(final String name) throws IOException {
        return delegate.appendEmptyTag(name);
    }

    @Override
    public HtmlBuilder<A> appendEscapedText(final String txt) throws IOException {
        delegate.appendEscapedText(txt);
        return this;
    }

    @Override
    public HtmlBuilder<A> appendUnescapedText(final String txt) throws IOException {
        delegate.appendUnescapedText(txt);
        return this;
    }

    @Override
    public A output() {
        return out;
    }

    @Override
    public HtmlBuilder<A> append(final CharSequence csq) throws IOException {
        out.append(csq);
        return this;
    }

    @Override
    public HtmlBuilder<A> append(final CharSequence csq, final int start, final int end) throws IOException {
        out.append(csq, start, end);
        return this;
    }

    @Override
    public HtmlBuilder<A> append(final char c) throws IOException {
        out.append(c);
        return this;
    }

    private abstract class AbstractHtmlBuilder implements HtmlBuilder<A> {

        @Override
        public final A output() {
            return out;
        }

        @Override
        @Deprecated
        public HtmlBuilder<A> append(CharSequence csq) throws IOException {
            out.append(csq);
            return this;
        }

        @Override
        @Deprecated
        public HtmlBuilder<A> append(CharSequence csq, int start, int end) throws IOException {
            out.append(csq, start, end);
            return this;
        }

        @Override
        @Deprecated
        public HtmlBuilder<A> append(char c) throws IOException {
            out.append(c);
            return this;
        }
    }

    private abstract class AbstractTagBuilder implements TagBuilder {

        private final boolean closeTag;

        AbstractTagBuilder(boolean closeTag) {
            this.closeTag = closeTag;
        }

        @Override
        public final TagBuilder appendAttribute(String name, String value) throws IOException {
            out.append(' ').append(name).append("=\"").append(textEscaper.escape(value)).append('\"');
            return this;
        }

        @Override
        public final TagBuilder appendBooleanAttribute(String name) throws IOException {
            out.append(' ').append(name);
            return this;
        }

        protected final void doCompleteTag() throws IOException {
            if (closeTag) {
                out.append('/');
            }
            out.append('>');
        }

        @Override
        @Deprecated
        public TagBuilder append(CharSequence csq) throws IOException {
            out.append(csq);
            return this;
        }

        @Override
        @Deprecated
        public TagBuilder append(CharSequence csq, int start, int end) throws IOException {
            out.append(csq, start, end);
            return this;
        }

        @Override
        @Deprecated
        public TagBuilder append(char c) throws IOException {
            out.append(c);
            return this;
        }
    }

    private final class FlatHtml extends AbstractHtmlBuilder {

        private final TagBuilder enclosingElementAttributes;
        private final TagBuilder emptyElementAttributes;

        private FlatHtml(boolean closeTags) {
            this.enclosingElementAttributes = new FlatTagBuilder(false);
            this.emptyElementAttributes = new FlatTagBuilder(closeTags);
        }

        @Override
        public TagBuilder appendStartTag(String name) throws IOException {
            out.append('<').append(name);
            return enclosingElementAttributes;
        }

        @Override
        public HtmlBuilder<A> appendEndTag(String name) throws IOException {
            out.append("</").append(name).append('>');
            return this;
        }

        @Override
        public TagBuilder appendEmptyTag(String name) throws IOException {
            out.append('<').append(name);
            return emptyElementAttributes;
        }

        @Override
        public HtmlBuilder<A> appendEscapedText(String txt) throws IOException {
            out.append(textEscaper.escape(txt));
            return this;
        }

        @Override
        public HtmlBuilder<A> appendUnescapedText(String txt) throws IOException {
            out.append(txt);
            return this;
        }

        private class FlatTagBuilder extends AbstractTagBuilder {

            private FlatTagBuilder(boolean closeTag) {
                super(closeTag);
            }

            @Override
            public HtmlBuilder<A> completeTag() throws IOException {
                super.doCompleteTag();
                return FlatHtml.this;
            }
        }
    }

    private final class IndentedHtml extends AbstractHtmlBuilder {

        private final String indent;
        private final TagBuilder enclosingElementAttributes;
        private final TagBuilder emptyElementAttributes;

        // Dealing with preformatted elements (pre and textarea) requires
        // that we know what our parent elements are. To do that we use
        // a stack; adding items as start tags are created, and removing them
        // as those tags are closed. Determining whether or not we are
        // currently rendering into a preformatted element is as simple as
        // asking if any tags on the stack match a preformatted element name.
        private final Deque<String> trace = new ArrayDeque<>();

        private IndentedHtml(String indent, boolean closeTags) {
            this.indent = indent;
            this.enclosingElementAttributes = new IndentedTagBuilder(false);
            this.emptyElementAttributes = new IndentedTagBuilder(closeTags);
        }

        private boolean isContentSelfFormatting() {
            return trace.contains("pre") || trace.contains("textarea");
        }

        private void appendIndent() throws IOException {
            for (int i = trace.size(); i > 0; i--) {
                out.append(indent);
            }
        }

        @Override
        public TagBuilder appendStartTag(String name) throws IOException {
            if (!isContentSelfFormatting()) {
                appendIndent();
            }

            trace.push(name);

            out.append('<').append(name);
            return enclosingElementAttributes;
        }

        @Override
        public HtmlBuilder<A> appendEndTag(String name) throws IOException {
            if (!isContentSelfFormatting()) {
                trace.pop();
                appendIndent();
            } else {
                trace.pop();
            }

            out.append("</").append(name).append('>');

            if (!isContentSelfFormatting()) {
                out.append('\n');
            }

            return this;
        }

        @Override
        public TagBuilder appendEmptyTag(String name) throws IOException {
            if (!isContentSelfFormatting()) {
                appendIndent();
            }
            out.append('<').append(name);
            return emptyElementAttributes;
        }

        private void appendLines(String txt) throws IOException {
            if (!isContentSelfFormatting()) {
                String[] lines = txt.split("\n");
                for (String line : lines) {
                    appendIndent();
                    out.append(line).append('\n');
                }
            } else {
                out.append(txt);
            }
        }

        @Override
        public HtmlBuilder<A> appendEscapedText(String txt) throws IOException {
            appendLines(textEscaper.escape(txt));
            return this;
        }

        @Override
        public HtmlBuilder<A> appendUnescapedText(String txt) throws IOException {
            appendLines(txt);
            return this;
        }

        private class IndentedTagBuilder extends AbstractTagBuilder {

            private IndentedTagBuilder(boolean closeTag) {
                super(closeTag);
            }

            @Override
            public HtmlBuilder<A> completeTag() throws IOException {
                super.doCompleteTag();

                if (!isContentSelfFormatting()) {
                    out.append('\n');
                }

                return IndentedHtml.this;
            }
        }
    }
}
