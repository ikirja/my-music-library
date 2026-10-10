package com.kirill.musiclibrary.domain

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Reproduces the LIBRARY.html formatting rules (plan §8.4). */
class FormatTest {

    @Test
    fun duration_isMinutesAndSeconds() {
        assertThat(Format.duration(341.50666666666666)).isEqualTo("5:42")
        assertThat(Format.duration(59.4)).isEqualTo("0:59")
        assertThat(Format.duration(60.0)).isEqualTo("1:00")
        assertThat(Format.duration(0.0)).isEqualTo("0:00")
    }

    @Test
    fun longDuration_usesHoursAboveAnHour() {
        assertThat(Format.longDuration(424690.0)).isEqualTo("117h 58m")
        assertThat(Format.longDuration(59.0)).isEqualTo("0:59")
        assertThat(Format.longDuration(3600.0)).isEqualTo("1h 00m")
    }

    @Test
    fun size_switchesDecimalsAtTenMb() {
        assertThat(Format.size(38871632L)).isEqualTo("37 MB")
        assertThat(Format.size(10_485_760L)).isEqualTo("10 MB")
        assertThat(Format.size(5_000_000L)).isEqualTo("4.8 MB")
    }

    @Test
    fun initials_takeFirstTwoWords() {
        assertThat(Format.initials("Opium")).isEqualTo("O")
        assertThat(Format.initials("Agata Kristi")).isEqualTo("AK")
        assertThat(Format.initials("L'Arc~en~Ciel")).isEqualTo("LA")
    }

    @Test
    fun hue_isDeterministicAndInRange() {
        val a = Format.hue("Opium")
        assertThat(a).isEqualTo(Format.hue("Opium"))
        assertThat(a).isAtLeast(0)
        assertThat(a).isLessThan(360)
    }

    @Test
    fun usb_badgeAndTrackNumber() {
        assertThat(Format.usb(null)).isEqualTo("N/A")
        assertThat(Format.usb(5)).isEqualTo("#5")
        assertThat(Format.trackNumber(1)).isEqualTo("01")
        assertThat(Format.trackNumber(null)).isEqualTo("—")
    }
}
