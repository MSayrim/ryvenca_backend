package com.ryvenca.color;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ColorScienceTest {

    /** Reference pairs from Sharma, Wu & Dalal (2005), "The CIEDE2000 color-difference formula". */
    @ParameterizedTest
    @CsvSource({
            "50.0000, 2.6772, -79.7751, 50.0000, 0.0000, -82.7485, 2.0425",
            "50.0000, 3.1571, -77.2803, 50.0000, 0.0000, -82.7485, 2.8615",
            "50.0000, -1.3802, -84.2814, 50.0000, 0.0000, -82.7485, 1.0000",
            "50.0000, 0.0000, 0.0000, 50.0000, -1.0000, 2.0000, 2.3669",
            "50.0000, 2.4900, -0.0010, 50.0000, -2.4900, 0.0009, 7.1792",
            "50.0000, 2.5000, 0.0000, 73.0000, 25.0000, -18.0000, 27.1492",
            "60.2574, -34.0099, 36.2677, 60.4626, -34.1751, 39.4387, 1.2644",
            "22.7233, 20.0904, -46.6940, 23.0331, 14.9730, -42.5619, 2.0373",
            "90.8027, -2.0831, 1.4410, 91.1528, -1.6435, 0.0447, 1.4441",
            "2.0776, 0.0795, -1.1350, 0.9033, -0.0636, -0.5514, 0.9082"
    })
    void deltaE2000MatchesReferenceData(double l1, double a1, double b1, double l2, double a2, double b2, double expected) {
        double de = ColorScience.deltaE2000(new Lab(l1, a1, b1), new Lab(l2, a2, b2));
        assertThat(de).isCloseTo(expected, within(1e-4));
        assertThat(ColorScience.deltaE2000(new Lab(l2, a2, b2), new Lab(l1, a1, b1))).isCloseTo(expected, within(1e-4));
    }

    @Test
    void labRoundTripKeepsTheColor() {
        for (String hex : new String[] {"#000000", "#FFFFFF", "#1F2A44", "#CDB89A", "#B8272E", "#6B6B3A", "#A9C6E3"}) {
            assertThat(ColorScience.labToHex(ColorScience.hexToLab(hex))).isEqualToIgnoringCase(hex);
        }
    }

    @Test
    void whiteAndBlackHaveExpectedLightness() {
        assertThat(ColorScience.hexToLab("#FFFFFF").l()).isCloseTo(100, within(0.01));
        assertThat(ColorScience.hexToLab("#000000").l()).isCloseTo(0, within(0.01));
        assertThat(ColorScience.hexToLab("#808080").chroma()).isLessThan(0.5);
    }

    @Test
    void hueDistanceWrapsAround() {
        assertThat(ColorScience.hueDistance(350, 10)).isEqualTo(20);
        assertThat(ColorScience.hueDistance(0, 180)).isEqualTo(180);
    }
}
