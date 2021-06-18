package j2html.tags;

import j2html.rendering.HtmlBuilder;
import java.io.IOException;
import org.junit.Test;

import static j2html.TagCreator.rawHtml;
import static j2html.TagCreator.text;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TextTest {

    @Test
    public void null_text_is_rendered_as_a_string_literal() throws IOException {
        assertThat(text(null).render(HtmlBuilder.inMemory()).toString(), is("null"));
        assertThat(text(null).render(HtmlBuilder.inMemory().withIndent("\t")).toString(), is("null\n"));
    }

    @Test
    public void null_unescaped_text_is_rendered_as_a_string_literal() throws IOException {
        assertThat(rawHtml(null).render(HtmlBuilder.inMemory()).toString(), is("null"));
        assertThat(rawHtml(null).render(HtmlBuilder.inMemory().withIndent("\t")).toString(), is("null\n"));
    }
}
