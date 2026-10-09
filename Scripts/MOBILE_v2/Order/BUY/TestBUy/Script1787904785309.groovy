import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords as Mobile
import com.kms.katalon.core.model.FailureHandling as FailureHandling
import com.kms.katalon.core.util.KeywordUtil as KeywordUtil
import internal.GlobalVariable as GlobalVariable
import java.time.ZonedDateTime as ZonedDateTime
import java.time.ZoneId as ZoneId
import java.time.format.DateTimeFormatter as DateTimeFormatter
import java.time.Instant as Instant
import java.time.Duration as Duration
import com.utilities.BionsSocketClient as BionsSocketClient
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.cucumber.keyword.CucumberBuiltinKeywords as CucumberKW
import com.kms.katalon.core.webservice.keyword.WSBuiltInKeywords as WS
import com.kms.katalon.core.windows.keyword.WindowsBuiltinKeywords as Windows
import static com.kms.katalon.core.testobject.ObjectRepository.findWindowsObject
import static com.kms.katalon.core.testdata.TestDataFactory.findTestData
import static com.kms.katalon.core.testcase.TestCaseFactory.findTestCase
import static com.kms.katalon.core.checkpoint.CheckpointFactory.findCheckpoint
import com.kms.katalon.core.testcase.TestCase as TestCase
import com.kms.katalon.core.testdata.TestData as TestData
import com.kms.katalon.core.testobject.TestObject as TestObject
import com.kms.katalon.core.checkpoint.Checkpoint as Checkpoint
import org.openqa.selenium.Keys as Keys

// ============================================================
// KONFIGURASI
// ============================================================
String applicationID = 'id.bions.bnis.android.new_bions_revamp'

String screenshotBasePath = '/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login'

//String userId = GlobalVariable.G_userIdDevuserId

//String password = GlobalVariable.G_userIdDevpassword

//String pin = GlobalVariable.G_userIdDevpin

//String host = GlobalVariable.G_userIdDevtradinghost

String userId = GlobalVariable.G_userIdDev
String password = GlobalVariable.G_passwordDev
String pin = GlobalVariable.G_pindev
String host = GlobalVariable.G_tradinghostDev
int feedPort = GlobalVariable.G_feedport.toInteger()
int tradingPort = GlobalVariable.G_tradingport.toInteger()
String clientIp = GlobalVariable.G_clientip
String deviceId = GlobalVariable.G_deviceId

String stockSymbol = 'BBNIRG'

int liveMonitorSeconds = 15 // durasi pantau live, bisa disesuaikan

int totalChecks = 0

int passedChecks = 0

def verify = { boolean condition, String description ->
	totalChecks++

	if (condition) {
		passedChecks++

		KeywordUtil.logInfo("✅ PASS: $description")
	} else {
		KeywordUtil.markWarning("❌ FAIL: $description")
	}
}

// ============================================================
// STEP 1: LAUNCH APPLICATION
// ============================================================
try {
	Mobile.startExistingApplication(applicationID, FailureHandling.STOP_ON_FAILURE)

	KeywordUtil.logInfo("Aplikasi dengan ID '$applicationID' berhasil diluncurkan.")
}
catch (Exception e) {
	KeywordUtil.markFailed('Gagal meluncurkan aplikasi. Pastikan aplikasi sudah terinstal di perangkat. Error: ' + e.getMessage())
}

// ============================================================
// STEP 2: LOGIN VIA UI MOBILE
// ============================================================
Mobile.setText(findTestObject('Login_firebase/User_id'), userId, 0)

Mobile.setText(findTestObject('Login_firebase/Pw'), password, 0)

Mobile.setText(findTestObject('Login_firebase/Pin'), pin, 0)

Mobile.takeScreenshot("$screenshotBasePath/Login0.PNG")

Instant start = Instant.now()

Mobile.tap(findTestObject('Login_V2/button_login'), 0)

Mobile.takeScreenshot("$screenshotBasePath/Login1.PNG")

Instant end = Instant.now()

double loginSeconds = Duration.between(start, end).toMillis() / 1000.0

KeywordUtil.logInfo("Waktu login sampai dashboard: $loginSeconds detik")

def now = ZonedDateTime.now(ZoneId.of('Asia/Jakarta'))

def fmt = DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss')

KeywordUtil.logInfo('Login successful at ' + now.format(fmt))

Mobile.takeScreenshot("$screenshotBasePath/Login_Biometric.PNG")

Mobile.tap(findTestObject('Login_V2/button_notnow'), 0)

Mobile.tap(findTestObject('NavBar_Scalability/trade_'), 0)

Mobile.takeScreenshot("$screenshotBasePath/Trade_V2.PNG")

//Mobile.swipe(500, 1500, 500, 500)

Mobile.takeScreenshot("$screenshotBasePath/Trade_V1.PNG")

Mobile.tap(findTestObject('BUY/Price'), 0)

Mobile.setText(findTestObject('BUY/Price'), '3780', 0)

Mobile.tap(findTestObject('BUY/LOT'), 0)

Mobile.setText(findTestObject('BUY/LOT'), '1', 0)

Mobile.tap(findTestObject('BUY/buy'), 0)

Mobile.tap(findTestObject('BUY/Confirm'), 0)

