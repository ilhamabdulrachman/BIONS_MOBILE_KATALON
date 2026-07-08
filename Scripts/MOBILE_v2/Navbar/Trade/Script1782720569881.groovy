import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords as Mobile
import static com.kms.katalon.core.checkpoint.CheckpointFactory.findCheckpoint
import static com.kms.katalon.core.testcase.TestCaseFactory.findTestCase
import static com.kms.katalon.core.testdata.TestDataFactory.findTestData
import static com.kms.katalon.core.testobject.ObjectRepository.findWindowsObject
import com.kms.katalon.core.checkpoint.Checkpoint as Checkpoint
import com.kms.katalon.core.cucumber.keyword.CucumberBuiltinKeywords as CucumberKW
import com.kms.katalon.core.model.FailureHandling as FailureHandling
import com.kms.katalon.core.testcase.TestCase as TestCase
import com.kms.katalon.core.testdata.TestData as TestData
import com.kms.katalon.core.testng.keyword.TestNGBuiltinKeywords as TestNGKW
import com.kms.katalon.core.testobject.TestObject as TestObject
import com.kms.katalon.core.webservice.keyword.WSBuiltInKeywords as WS
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.windows.keyword.WindowsBuiltinKeywords as Windows
import internal.GlobalVariable as GlobalVariable
import org.openqa.selenium.Keys as Keys
import com.utilities.TcpClient as TcpClient
import com.kms.katalon.core.util.KeywordUtil as KeywordUtil
import java.time.ZonedDateTime as ZonedDateTime
import java.time.ZoneId as ZoneId
import java.time.format.DateTimeFormatter as DateTimeFormatter
import java.time.Instant as Instant
import java.time.Duration as Duration
import com.utilities.TradingHours as TradingHours
import com.utilities.ShimmerWait as ShimmerWait
import groovy.json.JsonSlurper as JsonSlurper

boolean isMarketOpen = CustomKeywords.'com.utilities.TradingHours.isMarketOpen'()

if (isMarketOpen) {
	KeywordUtil.logInfo('Bursa sedang buka. Melanjutkan pengujian...')
} else {
	boolean isMarketBreak = CustomKeywords.'com.utilities.TradingHours.isMarketBreak'()

	if (isMarketBreak) {
		KeywordUtil.markFailed('Tes gagal. Bursa sedang istirahat.', FailureHandling.STOP_ON_FAILURE)
	} else {
		KeywordUtil.markFailed('Tes gagal. Bursa sedang tutup.', FailureHandling.STOP_ON_FAILURE)
	}
}
//def elemenDashboard = findTestObject('TEST_LOGIN/stock')
//NetworkChecker.verifyInternetConnection()
//Mobile.startApplication('/Users/bionsrevamp/Downloads/app-development-profile 1 (1).apk', true)
String applicationID = 'id.bions.bnis.android.new_bions_revamp'

try {
	Mobile.startExistingApplication(applicationID, FailureHandling.STOP_ON_FAILURE)

	KeywordUtil.logInfo("✅ Aplikasi dengan ID '$applicationID' berhasil diluncurkan.")
}
catch (Exception e) {
	KeywordUtil.markFailed('❌ Gagal meluncurkan aplikasi. Pastikan aplikasi sudah terinstal di perangkat. Error: ' + e.getMessage(),
		FailureHandling.STOP_ON_FAILURE)
}

Mobile.setText(findTestObject('Login_firebase/User_id'), 'REDACTED_USERID', 0)

Mobile.setText(findTestObject('Login_firebase/Pw'), 'REDACTED_PIN', 0)

Mobile.setText(findTestObject('Login_firebase/Pin'), 'REDACTED_PIN', 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Login0.PNG')

start = Instant.now()

Mobile.tap(findTestObject('Login_V2/login'), 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Login1.PNG')

TcpClient client = new TcpClient()

//client.connect('REDACTED_INTERNAL_IP', 62229 // FEED_SERVER_1
client.connect('Trade.Bions.id', 62229 // FEED_SERVER_1
	)

//  client.connect('mock.bions.xyz', 62229 // FEED_SERVER_1
// )
// Kirim login
//client.sendMessage('{ "action":"login", "user":"1B029", "password":"q" }')
client.sendMessage('{ "action":"login", "user":"REDACTED_USERID", "password":"REDACTED_PIN" }')

// Listen 5 detik untuk capture response login
client.listen(5)

// 🔌 Tutup koneksi
client.close()

end = Instant.now()

seconds = (Duration.between(start, end).toMillis() / 10000.0)

KeywordUtil.logInfo("⏱️ Waktu login sampai dashboard: $seconds detik")

def now = ZonedDateTime.now(ZoneId.of('Asia/Jakarta'))

def fmt = DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss')

KeywordUtil.logInfo('Login successful at ' + now.format(fmt))

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Login_Biometric.PNG')

Mobile.tap(findTestObject('Login_firebase/Not_now'), 0)

Mobile.tap(findTestObject('NavBar_Scalability/trade'), 0)

Mobile.swipe(500, 1500, 500, 500)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Login1_V2.PNG')


Mobile.closeApplication()

