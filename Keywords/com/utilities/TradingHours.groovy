package com.utilities

import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.util.KeywordUtil

import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.ZoneId

public class TradingHours {

	@Keyword
	def static boolean isMarketOpen() {
		def jakartaZone = ZoneId.of('Asia/Jakarta')
		def currentTime = ZonedDateTime.now(jakartaZone)
		def currentDay = currentTime.getDayOfWeek()
		def localTime = currentTime.toLocalTime()

		// Bursa tutup pada akhir pekan (Sabtu & Minggu)
		if (currentDay == DayOfWeek.SATURDAY || currentDay == DayOfWeek.SUNDAY) {
			KeywordUtil.logInfo("Bursa tutup pada akhir pekan.")
			return false
		}
		// --- Aturan untuk PRE-OPENING (Senin-Jumat, 08:45 - 08:55) ---
		def preOpeningStart = LocalTime.of(8, 45)
		def preOpeningEnd = LocalTime.of(8, 55)
		if ((localTime.isAfter(preOpeningStart) || localTime.equals(preOpeningStart)) && localTime.isBefore(preOpeningEnd)) {
			KeywordUtil.logInfo("Bursa sedang dalam sesi Pre-opening.")
			return true
		}


		// Aturan khusus untuk hari Jumat
		if (currentDay == DayOfWeek.FRIDAY) {
			// Jam Sesi I (09:00 - 11:30)
			def fridaySession1Start = LocalTime.of(9, 0)
			def fridaySession1End = LocalTime.of(11, 30)

			// Jam Sesi II (14:00 - 16:00)
			def fridaySession2Start = LocalTime.of(14, 0)
			def fridaySession2End = LocalTime.of(16, 0)
			
			// Jam Sesi Pre-closing (14:50 - 15:00)
			def fridayPreClosingStart = LocalTime.of(14, 50)
			def fridayPreClosingEnd = LocalTime.of(15, 0)

			if ((localTime.isAfter(fridaySession1Start) || localTime.equals(fridaySession1Start)) && localTime.isBefore(fridaySession1End)) {
				KeywordUtil.logInfo("Bursa sedang buka pada Sesi I (Jumat).")
				return true
			}

			if ((localTime.isAfter(fridaySession2Start) || localTime.equals(fridaySession2Start)) && localTime.isBefore(fridaySession2End)) {
				KeywordUtil.logInfo("Bursa sedang buka pada Sesi II (Jumat).")
				return true
			}
		} else {
			// Aturan untuk Senin - Kamis
			// Jam Sesi I (09:00 - 12:00)
			def session1Start = LocalTime.of(9, 0)
			def session1End = LocalTime.of(12, 0)

			// Jam Sesi II (13:30 - 16:00)
			def session2Start = LocalTime.of(13, 30)
			def session2End = LocalTime.of(16, 0)
			
			// Jam Sesi Pre-closing (15:50 - 16:00)
			def preClosingStart = LocalTime.of(15, 50)
			def preClosingEnd = LocalTime.of(16, 0)

			if ((localTime.isAfter(session1Start) || localTime.equals(session1Start)) && localTime.isBefore(session1End)) {
				KeywordUtil.logInfo("Bursa sedang buka pada Sesi I (Senin-Kamis).")
				return true
			}
			
			if ((localTime.isAfter(preClosingStart) || localTime.equals(preClosingStart)) && localTime.isBefore(preClosingEnd)) {
				KeywordUtil.logInfo("Bursa sedang dalam sesi Pre-closing (Senin-Kamis).")
				return true
			}

			if ((localTime.isAfter(session2Start) || localTime.equals(session2Start)) && localTime.isBefore(session2End)) {
				KeywordUtil.logInfo("Bursa sedang buka pada Sesi II (Senin-Kamis).")
				return true
			}
		}

		KeywordUtil.logInfo("Bursa sedang tutup.")
		return false
	}
	@Keyword
	def static boolean isSbnSecondaryAllowed() {
		def jakartaZone = ZoneId.of('Asia/Jakarta')
		def currentTime = ZonedDateTime.now(jakartaZone)
		def currentDay = currentTime.getDayOfWeek()
		def localTime = currentTime.toLocalTime()

		// Tutup Sabtu & Minggu
		if (currentDay == DayOfWeek.SATURDAY || currentDay == DayOfWeek.SUNDAY) {
			KeywordUtil.logInfo("SBN Secondary tidak tersedia (akhir pekan).")
			return false
		}

		// Sesi I (Senin-Jumat, 09:00–11:30)
		def sesi1Start = LocalTime.of(9, 0)
		def sesi1End   = LocalTime.of(11, 30)

		if (!localTime.isBefore(sesi1Start) && localTime.isBefore(sesi1End)) {
			KeywordUtil.logInfo("✅ SBN Secondary diperbolehkan (Sesi I).")
			return true
		}

		KeywordUtil.logInfo("❌ SBN Secondary hanya bisa di Sesi I (09:00–11:30).")
		return false
	}
}