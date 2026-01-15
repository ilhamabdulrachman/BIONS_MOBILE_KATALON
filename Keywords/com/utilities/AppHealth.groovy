package com.utilities
import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.driver.DriverFactory
import io.appium.java_client.AppiumDriver

class AppHealth {

	@Keyword
	static void verifyAppIsAlive(String expectedPackage) {
		try {
			AppiumDriver driver = DriverFactory.getMobileDriver()
			String currentPackage = driver.getCurrentPackage()

			if (currentPackage == null || !currentPackage.equals(expectedPackage)) {
				KeywordUtil.markFailedAndStop(
						"❌ Aplikasi ${expectedPackage} TIDAK AKTIF / CRASH. Current: ${currentPackage}"
						)
			}

			KeywordUtil.logInfo("✅ Aplikasi ${expectedPackage} masih berjalan normal")
		} catch (Exception e) {
			KeywordUtil.markFailedAndStop(
					"❌ Aplikasi CRASH / tidak respons: ${e.message}"
					)
		}
	}
}
