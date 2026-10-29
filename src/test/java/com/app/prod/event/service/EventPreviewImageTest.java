package com.app.prod.event.service;

import com.app.prod.event.dto.EventPreview;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Set {@code -Dpreview.samples=<dir>} to have the rendered images written out and look at them -
 * a test cannot tell whether a title is pleasant to read.
 */
class EventPreviewImageTest {

    private final EventPreviewImage renderer = new EventPreviewImage();

    @Test
    void ordinaryEvent() throws IOException {
        assertRenders("ordinary", preview("Planszówki u Kasi", "sobota, 15 listopada · 19:00–23:00",
                "Klub Pod Wiatrakiem, ul. Żółkiewskiego 12", "Idzie 12 osób"));
    }

    @Test
    void titleTooLongForThreeLinesIsCut() throws IOException {
        assertRenders("long-title", preview(
                "Wielki turniej siatkówki plażowej dla wszystkich mieszkańców osiedla, ich rodzin, znajomych i sąsiadów z bloku obok, z grillem i nagrodami",
                "Termin w głosowaniu do 31 października, 22:00", null, "Idzie 1 osoba"));
    }

    @Test
    void wordWiderThanTheImageIsBroken() throws IOException {
        assertRenders("one-long-word", preview("Zażółćgęśląjaźń".repeat(6), null, null, null));
    }

    @Test
    void emojiAreLeftOutInsteadOfDrawnAsBoxes() throws IOException {
        assertRenders("emoji", preview("🎉 Urodziny Ani 🎂", "piątek, 1 listopada · 20:00", "Online", null));
    }

    @Test
    void linkThatLeadsNowhere() throws IOException {
        assertRenders("missing", new EventPreview(false, "Unitly", "Unitly", null, null, null,
                "Umawiaj się ze znajomymi i wybierzcie termin, który pasuje wszystkim.", "", "", "pl_PL"));
    }

    @Test
    void polishHasThreeFormsOfPeople() {
        assertThat(EventPreviewService.pluralForm(1)).isEqualTo("one");
        assertThat(EventPreviewService.pluralForm(2)).isEqualTo("few");
        assertThat(EventPreviewService.pluralForm(4)).isEqualTo("few");
        assertThat(EventPreviewService.pluralForm(5)).isEqualTo("many");
        assertThat(EventPreviewService.pluralForm(12)).isEqualTo("many");
        assertThat(EventPreviewService.pluralForm(14)).isEqualTo("many");
        assertThat(EventPreviewService.pluralForm(22)).isEqualTo("few");
        assertThat(EventPreviewService.pluralForm(112)).isEqualTo("many");
    }

    private void assertRenders(String name, EventPreview preview) throws IOException {
        byte[] png = renderer.render(preview);

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
        assertThat(image.getWidth()).isEqualTo(EventPreviewImage.WIDTH);
        assertThat(image.getHeight()).isEqualTo(EventPreviewImage.HEIGHT);
        assertThat(png.length).as("WhatsApp drops images much past 300 kB").isLessThan(300 * 1024);

        String samples = System.getProperty("preview.samples");
        if (samples != null) {
            Files.createDirectories(Path.of(samples));
            Files.write(Path.of(samples, name + ".png"), png);
        }
    }

    private static EventPreview preview(String title, String when, String where, String going) {
        return new EventPreview(true, "Unitly", title, when, where, going, "", "", "", "pl_PL");
    }
}
