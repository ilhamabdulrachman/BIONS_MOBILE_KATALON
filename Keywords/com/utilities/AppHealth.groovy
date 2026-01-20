package com.utilities

import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords as Mobile
import com.kms.katalon.core.util.KeywordUtil
import java.security.MessageDigest
import java.nio.file.Files
import java.nio.file.Paths

class AppHealth {

    @Keyword
    static void verifyAppIsAlive(String expectedPackage) {
        try {
            String os = Mobile.getDeviceOS()

            if (os == null || os.isEmpty()) {
                KeywordUtil.markFailedAndStop(
                    "❌ App Health check gagal: Mobile session belum aktif"
                )
            }

            KeywordUtil.logInfo("✅ App masih berjalan normal (OS: ${os})")
        } catch (Exception e) {
            KeywordUtil.markFailedAndStop(
                "❌ Aplikasi CRASH / tidak respons: ${e.message}"
            )
        }
    }
}

class FreezeDetector {
	
		@Keyword
		static void detectFrozenScreen(int timeoutSeconds, int checkIntervalSeconds) {
	
			long start = System.currentTimeMillis()
			String lastHash = null
	
			while ((System.currentTimeMillis() - start) < timeoutSeconds * 1000) {
	
				String path = System.getProperty("user.dir") +
						"/Reports/freeze_${System.currentTimeMillis()}.png"
	
				Mobile.takeScreenshot(path)
	
				byte[] bytes = Files.readAllBytes(Paths.get(path))
				String hash = md5(bytes)
	
				if (hash == lastHash) {
					KeywordUtil.markFailedAndStop(
						"❌ FREEZE TERDETEKSI: UI tidak berubah selama ${checkIntervalSeconds} detik"
					)
				}
	
				lastHash = hash
				Mobile.delay(checkIntervalSeconds)
			}
	
			KeywordUtil.logInfo("✅ Tidak terdeteksi freeze")
		}
	
		private static String md5(byte[] data) {
			MessageDigest md = MessageDigest.getInstance("MD5")
			return md.digest(data).encodeHex().toString()
		}
	}
