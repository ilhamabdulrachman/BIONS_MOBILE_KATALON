package com.utilities

import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords as Mobile
import com.kms.katalon.core.util.KeywordUtil
import java.security.MessageDigest
import java.nio.file.Files
import java.nio.file.Paths

class AppHealth {

 
    @Keyword
    static void verifyAppIsAlive() {
        try {
            String os = Mobile.getDeviceOS()

            if (os == null || os.isEmpty()) {
                KeywordUtil.markFailedAndStop(
                        "❌ App Health gagal: Mobile session tidak aktif"
                )
            }

            KeywordUtil.logInfo("✅ App masih hidup (OS: ${os})")
        } catch (Exception e) {
            KeywordUtil.markFailedAndStop(
                    "❌ Aplikasi CRASH / tidak respons: ${e.message}"
            )
        }
    }


    @Keyword
    static boolean observeAppHealth() {
        try {
            String os = Mobile.getDeviceOS()
            return (os == null || os.isEmpty())
        } catch (Exception e) {
            return true
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

            try {
                Mobile.takeScreenshot(path)
            } catch (Exception e) {
                KeywordUtil.markFailedAndStop(
                        "❌ CRASH TERDETEKSI: Tidak bisa ambil screenshot (session mati)"
                )
            }

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

        KeywordUtil.logInfo("✅ Tidak terdeteksi freeze (STRICT MODE)")
    }


   @Keyword
    static boolean detectFrozenOrCrashedScreenObserver(
            int timeoutSeconds,
            int checkIntervalSeconds) {

        long startTime = System.currentTimeMillis()
        String lastHash = null

        while ((System.currentTimeMillis() - startTime) < timeoutSeconds * 1000) {

            String path = System.getProperty("user.dir") +
                "/Reports/freeze_${System.currentTimeMillis()}.png"

            try {
                Mobile.takeScreenshot(path)
            } catch (Exception e) {
                KeywordUtil.markWarning(
                    "💥 CRASH TERDETEKSI: Tidak bisa ambil screenshot"
                )
                return true
            }

            byte[] bytes = Files.readAllBytes(Paths.get(path))
            String currentHash = generateMD5(bytes)

            if (currentHash == lastHash) {
                KeywordUtil.markWarning(
                    "🧊 FREEZE TERDETEKSI: UI tidak berubah"
                )
                return true
            }

            lastHash = currentHash
            Mobile.delay(checkIntervalSeconds)
        }

        KeywordUtil.logInfo("✅ App normal (observer mode)")
        return false
    }


    private static String generateMD5(byte[] data) {
        MessageDigest md = MessageDigest.getInstance("MD5")
        return md.digest(data).encodeHex().toString()
    }
}
