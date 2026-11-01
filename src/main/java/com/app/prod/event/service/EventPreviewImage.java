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

/**
 * Draws the picture a chat app shows under a shared event link: 1200x630, the size every one of them
 * crops to without cutting anything off. Only the site name and the event's title - the date and the
 * place are in the text the chat app prints next to it. Colours and font are the ones of the mails.
 * <p>
 * The font comes from the classpath rather than from the system, so the image looks the same on a
 * laptop and in the container. Characters it cannot draw - emoji above all - are left out instead
 * of turning into empty boxes.
 */
@Component
public class EventPreviewImage {

    public static final int WIDTH = 1200;
    public static final int HEIGHT = 630;
    private static final int MARGIN = 72;
    private static final int TEXT_WIDTH = WIDTH - 2 * MARGIN;
    private static final int SITE_NAME_BASELINE = 112;
    private static final int TITLE_TOP = 144;
    private static final int TITLE_BOTTOM = HEIGHT - MARGIN;
    private static final float[] TITLE_SIZES = {136f, 112f, 94f, 80f, 68f};

    private static final Color BACKGROUND = new Color(0xf7f3ee);
    private static final Color INK = new Color(0x1e1a16);
    private static final Color ACCENT = new Color(0xc0563a);

    private final Font serif;

    public EventPreviewImage() {
        this.serif = load("fonts/DMSerifDisplay-Regular.ttf");
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
            g.setFont(serif.deriveFont(56f));
            g.drawString(displayable(g.getFont(), preview.siteName()), MARGIN, SITE_NAME_BASELINE);

            String title = preview.found() ? preview.title() : preview.description();
            drawTitle(g, displayable(serif, title));
        } finally {
            g.dispose();
        }
        return png(image);
    }

    /** The biggest size the title fits at, centred below the site name; past the smallest one it is cut with an ellipsis. */
    private void drawTitle(Graphics2D g, String title) {
        g.setColor(INK);
        for (int i = 0; i < TITLE_SIZES.length; i++) {
            Font font = serif.deriveFont(TITLE_SIZES[i]);
            FontMetrics metrics = g.getFontMetrics(font);
            int lineHeight = Math.round(TITLE_SIZES[i] * 1.1f);
            int textHeight = metrics.getAscent() + metrics.getDescent();
            int maxLines = Math.max(1, 1 + (TITLE_BOTTOM - TITLE_TOP - textHeight) / lineHeight);
            List<String> lines = wrap(title, metrics);
            boolean last = i == TITLE_SIZES.length - 1;

            if (lines.size() <= maxLines || last) {
                lines = fit(lines, maxLines, metrics);
                g.setFont(font);
                int blockHeight = textHeight + (lines.size() - 1) * lineHeight;
                int baseline = TITLE_TOP + (TITLE_BOTTOM - TITLE_TOP - blockHeight) / 2 + metrics.getAscent();
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
