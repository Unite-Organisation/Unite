package com.app.prod.event.service;

import com.app.prod.event.dto.EventPreview;
import com.app.prod.exceptions.AppError;
import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.IllegalApplicationStateException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Draws the picture a chat app shows under a shared event link: 1200x630, the size every one of them
 * crops to without cutting anything off. Colours and fonts are the ones of the mails.
 * <p>
 * Fonts come from the classpath rather than from the system, so the image looks the same on a
 * laptop and in the container. Characters they cannot draw - emoji above all - are left out instead
 * of turning into empty boxes.
 */
@Component
public class EventPreviewImage {

    public static final int WIDTH = 1200;
    public static final int HEIGHT = 630;

    // a chat app shows the image about 330 px wide - a quarter of its size - so the text is sized for that
    private static final int MARGIN = 72;
    private static final int TEXT_WIDTH = WIDTH - 2 * MARGIN;
    private static final int SITE_NAME_BASELINE = 104;
    private static final int TITLE_TOP = 136;
    private static final int TITLE_GAP = 48;
    private static final float[] TITLE_SIZES = {136f, 112f, 94f, 80f, 68f};
    private static final float FACT_LEADING = 1.15f;
    private static final int FACT_GAP = 8;

    private static final Color BACKGROUND = new Color(0xf7f3ee);
    private static final Color INK = new Color(0x1e1a16);
    private static final Color MUTED = new Color(0x7a6f64);
    private static final Color ACCENT = new Color(0xc0563a);

    private final Font serif;
    private final Font sans;
    private final Font sansBold;

    public EventPreviewImage() {
        this.serif = load("fonts/DMSerifDisplay-Regular.ttf");
        this.sans = load("fonts/Outfit-Regular.ttf");
        this.sansBold = load("fonts/Outfit-SemiBold.ttf");
    }

    public byte[] render(EventPreview preview) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            g.setColor(BACKGROUND);
            g.fillRect(0, 0, WIDTH, HEIGHT);
            g.setColor(ACCENT);
            g.fillRect(0, 0, 16, HEIGHT);

            g.setFont(serif.deriveFont(48f));
            g.drawString(displayable(g.getFont(), preview.siteName()), MARGIN, SITE_NAME_BASELINE);

            int factsTop = drawFacts(g, preview);
            String title = preview.found() ? preview.title() : preview.description();
            drawTitle(g, displayable(serif, title), factsTop - TITLE_GAP);
        } finally {
            g.dispose();
        }
        return png(image);
    }

    /**
     * Bottom up, so the title above gets whatever room the facts leave. The date may take two lines -
     * "Termin w głosowaniu do 1 listopada, 20:00" does not fit one at this size. Returns the top of the block.
     */
    private int drawFacts(Graphics2D g, EventPreview preview) {
        record Fact(String text, Font font, Color color, int maxLines) {
        }
        List<Fact> facts = Stream.of(
                        new Fact(preview.when(), sansBold.deriveFont(56f), INK, 2),
                        new Fact(preview.where(), sans.deriveFont(50f), MUTED, 1))
                .filter(fact -> fact.text() != null && !fact.text().isBlank())
                .toList();

        int bottom = HEIGHT - MARGIN + 16;
        for (Fact fact : facts.reversed()) {
            FontMetrics metrics = g.getFontMetrics(fact.font());
            int lineHeight = Math.round(fact.font().getSize2D() * FACT_LEADING);
            List<String> lines = fit(wrap(displayable(fact.font(), fact.text()), metrics), fact.maxLines(), metrics);

            g.setFont(fact.font());
            g.setColor(fact.color());
            int baseline = bottom - (lines.size() - 1) * lineHeight;
            for (String line : lines) {
                g.drawString(line, MARGIN, baseline);
                baseline += lineHeight;
            }
            bottom -= lines.size() * lineHeight + FACT_GAP;
        }
        return bottom - (facts.isEmpty() ? 0 : Math.round(50f * 0.8f));
    }

    /** The biggest size the title fits at; past the smallest one it is cut with an ellipsis. */
    private void drawTitle(Graphics2D g, String title, int bottom) {
        g.setColor(INK);
        for (int i = 0; i < TITLE_SIZES.length; i++) {
            Font font = serif.deriveFont(TITLE_SIZES[i]);
            FontMetrics metrics = g.getFontMetrics(font);
            int lineHeight = Math.round(TITLE_SIZES[i] * 1.1f);
            // the first line takes its ascent, every next one a line height; the descent of the last
            // one hangs into TITLE_GAP
            int maxLines = Math.max(1, 1 + (bottom - TITLE_TOP - metrics.getAscent()) / lineHeight);
            List<String> lines = wrap(title, metrics);
            boolean last = i == TITLE_SIZES.length - 1;

            if (lines.size() <= maxLines || last) {
                lines = fit(lines, maxLines, metrics);
                g.setFont(font);
                int baseline = TITLE_TOP + metrics.getAscent();
                for (String line : lines) {
                    g.drawString(line, MARGIN, baseline);
                    baseline += lineHeight;
                }
                return;
            }
        }
    }

    private static List<String> wrap(String text, FontMetrics metrics) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();

        for (String word : text.split("\\s+")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (metrics.stringWidth(candidate) <= TEXT_WIDTH) {
                line = new StringBuilder(candidate);
                continue;
            }
            if (!line.isEmpty()) {
                lines.add(line.toString());
            }
            // a word wider than the whole line has to break somewhere
            line = new StringBuilder();
            for (int codePoint : word.codePoints().toArray()) {
                if (!line.isEmpty() && metrics.stringWidth(line + Character.toString(codePoint)) > TEXT_WIDTH) {
                    lines.add(line.toString());
                    line = new StringBuilder();
                }
                line.appendCodePoint(codePoint);
            }
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }

    /** At most {@code maxLines}, the last one cut with an ellipsis when something was left out. */
    private static List<String> fit(List<String> lines, int maxLines, FontMetrics metrics) {
        if (lines.size() <= maxLines) {
            return lines;
        }
        List<String> kept = new ArrayList<>(lines.subList(0, maxLines));
        kept.set(maxLines - 1, ellipsize(kept.get(maxLines - 1) + "…", metrics));
        return kept;
    }

    private static String ellipsize(String text, FontMetrics metrics) {
        if (metrics.stringWidth(text) <= TEXT_WIDTH) {
            return text;
        }
        String cut = text.endsWith("…") ? text.substring(0, text.length() - 1) : text;
        while (!cut.isEmpty() && metrics.stringWidth(cut.stripTrailing() + "…") > TEXT_WIDTH) {
            cut = cut.substring(0, cut.offsetByCodePoints(cut.length(), -1));
        }
        return cut.stripTrailing() + "…";
    }

    private static String displayable(Font font, String text) {
        if (text == null) {
            return "";
        }
        StringBuilder kept = new StringBuilder();
        text.codePoints()
                .map(codePoint -> Character.isWhitespace(codePoint) ? ' ' : codePoint)
                .filter(codePoint -> codePoint == ' ' || font.canDisplay(codePoint))
                .forEach(kept::appendCodePoint);
        return kept.toString().replaceAll(" {2,}", " ").strip();
    }

    private static byte[] png(BufferedImage image) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalApplicationStateException(AppError.of(Code.EVENT_PREVIEW_RENDERING_FAILED, e.getMessage()));
        }
    }

    private static Font load(String path) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return Font.createFont(Font.TRUETYPE_FONT, in);
        } catch (IOException | FontFormatException e) {
            throw new IllegalApplicationStateException(AppError.of(Code.EVENT_PREVIEW_RENDERING_FAILED,
                    String.format("Font %s could not be loaded: %s", path, e.getMessage())));
        }
    }
}
