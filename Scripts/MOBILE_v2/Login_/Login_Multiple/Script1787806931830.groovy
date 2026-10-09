import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import static com.kms.katalon.core.testdata.TestDataFactory.findTestData
import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords as Mobile
import com.kms.katalon.core.model.FailureHandling as FailureHandling
import com.kms.katalon.core.util.KeywordUtil as KeywordUtil
import com.kms.katalon.core.testdata.TestData as TestData
import internal.GlobalVariable as GlobalVariable
import java.time.ZonedDateTime as ZonedDateTime
import java.time.ZoneId as ZoneId
import java.time.format.DateTimeFormatter as DateTimeFormatter
import java.time.Instant as Instant
import java.time.Duration as Duration
import com.utilities.BionsSocketClient as BionsSocketClient
import com.utilities.NetworkDiagnostic

// ============================================================
// KONFIGURASI
// ============================================================
String applicationID = 'id.bions.bnis.android.new_bions_revamp'
String screenshotBasePath = '/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login'

String host = GlobalVariable.G_userIdDevtradinghost
int feedPort = GlobalVariable.G_userIdDevfeedport.toInteger()
int tradingPort = GlobalVariable.G_userIdDevtradingport.toInteger()
String clientIp = GlobalVariable.G_userIdDevclientip
String deviceId = GlobalVariable.G_userIdDevdeviceId

// ===== AMBIL DATA SEMUA USER DARI DATA FILE =====
TestData usersData = findTestData('Data Files/user_login')
int totalUsers = usersData.getRowNumbers()

KeywordUtil.logInfo("📋 Total user yang akan dites: ${totalUsers}")

int successCount = 0
int failCount = 0

// ============================================================
// LOOP 
// ============================================================
for (int row = 1; row <= totalUsers; row++) {

	String userId = usersData.getValue('userId', row)
	String password = usersData.getValue('password', row)
	String pin = usersData.getValue('pin', row)

	KeywordUtil.logInfo('='.multiply(60))
	KeywordUtil.logInfo("👤 USER ${row}/${totalUsers}: ${userId}")
	KeywordUtil.logInfo('='.multiply(60))

	try {
		// ===== LAUNCH APPLICATION =====
		Mobile.startExistingApplication(applicationID, FailureHandling.STOP_ON_FAILURE)
		KeywordUtil.logInfo("Aplikasi berhasil diluncurkan untuk user ${userId}.")

		// ===== LOGIN VIA UI MOBILE =====
		Mobile.setText(findTestObject('Login_firebase/User_id'), userId, 0)
		Mobile.setText(findTestObject('Login_firebase/Pw'), password, 0)
		Mobile.setText(findTestObject('Login_firebase/Pin'), pin, 0)
		Mobile.takeScreenshot("${screenshotBasePath}/Login0_${userId}.PNG")

		Instant loginStart = Instant.now()
		Mobile.tap(findTestObject('Login_V2/button_login'), 0)
		Mobile.takeScreenshot("${screenshotBasePath}/Login1_${userId}.PNG")

		boolean loginSuccess = Mobile.verifyElementExist(
				findTestObject('Login_V2/button_notnow'),
				10,
				FailureHandling.OPTIONAL
		)

		Instant loginEnd = Instant.now()
		double loginSeconds = Duration.between(loginStart, loginEnd).toMillis() / 1000.0

		if (!loginSuccess) {
			Mobile.takeScreenshot("${screenshotBasePath}/Login_FAILED_${userId}.PNG")
			Map diagnostic = NetworkDiagnostic.runDiagnostic(host, feedPort)
			KeywordUtil.logInfo("❌ Login GAGAL untuk user ${userId}. Diagnosa: ${diagnostic.diagnosis}")
			failCount++
			Mobile.closeApplication()
			continue   
		}

		KeywordUtil.logInfo("✅ Login berhasil untuk ${userId} (${loginSeconds} detik)")
		successCount++

		// ===== HANDLE BIOMETRIC PROMPT =====
		Mobile.tap(findTestObject('Login_V2/button_notnow'), 0)
		Mobile.takeScreenshot("${screenshotBasePath}/Login_V2_${userId}.PNG")

		// ===== SOCKET FEED LOGIN (contoh verifikasi tambahan per user) =====
		BionsSocketClient feedClient = new BionsSocketClient()
		try {
			Map feedResult = feedClient.loginFeed(host, feedPort, userId, password, clientIp)
			KeywordUtil.logInfo("✅ Feed Login berhasil untuk ${userId}! Gateway: ${feedResult.gatewayId}")
		} catch (Exception e) {
			KeywordUtil.logInfo("❌ Feed Login gagal untuk ${userId}: ${e.message}")
		} finally {
			feedClient.closeSocket()
		}

		// ===== TUTUP APLIKASI SEBELUM LANJUT KE USER BERIKUTNYA =====
		Mobile.closeApplication()

	} catch (Exception e) {
		KeywordUtil.logInfo("❌ ERROR tak terduga untuk user ${userId}: ${e.message}")
		failCount++
		try {
			Mobile.closeApplication()
		} catch (Exception ignored) {
			
		}
	}

	
	Mobile.delay(5)
}

// ============================================================
// RINGKASAN AKHIR SEMUA USER
// ============================================================
KeywordUtil.logInfo('='.multiply(60))
KeywordUtil.logInfo('📊 RINGKASAN HASIL SEMUA USER')
KeywordUtil.logInfo('='.multiply(60))
KeywordUtil.logInfo("Total User    : ${totalUsers}")
KeywordUtil.logInfo("✅ Berhasil   : ${successCount}")
KeywordUtil.logInfo("❌ Gagal      : ${failCount}")
KeywordUtil.logInfo('='.multiply(60))