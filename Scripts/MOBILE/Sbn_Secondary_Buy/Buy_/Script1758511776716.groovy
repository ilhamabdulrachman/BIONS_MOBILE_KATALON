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

boolean isMarketOpen = CustomKeywords.'com.utilities.TradingHours.isMarketOpen'()

if (isMarketOpen) {
    KeywordUtil.logInfo('Bursa sedang buka. Melanjutkan pengujian login...' // Jika bursa tutup, hentikan tes
        )
} else {
    KeywordUtil.markFailed('Tes gagal. Bursa sedang tutup.', FailureHandling.STOP_ON_FAILURE)
}

def elemenDashboard = findTestObject('TEST_LOGIN/stock')

def productdetail = findTestObject('SBN/product_detail')

//NetworkChecker.verifyInternetConnection()
Mobile.startApplication('/Users/bionsrevamp/Downloads/app-development-profile 1 (1).apk', true)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/LOGIN.PNG', FailureHandling.STOP_ON_FAILURE)

//NetworkChecker.verifyInternetConnection()
Mobile.tap(findTestObject('TEST_LOGIN/skip_onboarding'), 0)

Mobile.setText(findTestObject('Login_firebase/User_id'), '1B029', 0)

Mobile.setText(findTestObject('Login_firebase/Pw'), 'q', 0)

Mobile.setText(findTestObject('Login_firebase/Pin'), 'q12345', 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Login0.PNG')

start = Instant.now()

Mobile.tap(findTestObject('TEST_LOGIN/btn_'), 0)

def now = ZonedDateTime.now(ZoneId.of('Asia/Jakarta'))

def fmt = DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss')

KeywordUtil.logInfo('Login successful at ' + now.format(fmt))

//NetworkChecker.verifyInternetConnection()
Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Login1.PNG')

TcpClient client = new TcpClient()

client.connect('192.168.19.61', 62229 // FEED_SERVER_1
    )

// Kirim login
client.sendMessage('{ "action":"login", "user":"1B029", "password":"q" }')

// Listen 5 detik untuk capture response login
client.listen(5)

// 🔌 Tutup koneksi
client.close()

end = Instant.now()

seconds = (Duration.between(start, end).toMillis() / 1000)

KeywordUtil.logInfo("⏱️ Waktu login sampai dashboard: $seconds detik")

Mobile.delay(2, FailureHandling.STOP_ON_FAILURE)

Mobile.tap(findTestObject('TEST_LOGIN/SKIP_QUIK_TOUR'), 0)

ShimmerWait.waitForShimmerToDisappear(elemenDashboard, 3)

Mobile.tap(findTestObject('SBN/Tap_Fixed_Income'), 1)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Fixedincomesecondary.PNG')

Mobile.tap(findTestObject('SBN/Klik_Sbn'), 1)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Fixedincomesecondary1.PNG')

Mobile.tap(findTestObject('SBN/Button_Secondary'), 0)
Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Fixedincomesecondary2.PNG')

Mobile.tap(findTestObject('SBN/ori002'), 0)
Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Fixedincomesecondary3.PNG')

ShimmerWait.waitForShimmerToDisappear(productdetail, 3)

Mobile.tap(findTestObject('SBN/Buy_secondary'), 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Fixedincomesecondary4.PNG')


Mobile.tap(findTestObject('SBN/klik_tambah_secondary'), 0, FailureHandling.STOP_ON_FAILURE)

boolean allowed = CustomKeywords.'com.utilities.TradingHours.isSbnSecondaryAllowed'()

if (!allowed) {
	KeywordUtil.markFailedAndStop("❌ Order SBN Secondary hanya bisa di Sesi 1 .")
}

Mobile.tap(findTestObject('SBN/buy_ijo_secondary'), 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Fixedincomesecondary5.PNG')


start = Instant.now()

Mobile.tap(findTestObject('SBN/button_confirm_submit'), 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Fixedincomesecondary5.PNG')

end = Instant.now()

long seconds = Duration.between(start, end).toMillis() / 1000

KeywordUtil.markPassed("⏱️ Order terkirim dalam waktu $seconds detik")

KeywordUtil.logInfo('Order Sent at ' + now.format(fmt))

Instant start1 = Instant.now()

Mobile.tap(findTestObject('SBN/view_order_list_secondary'), 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Fixedincomesecondary6.PNG')

Instant end1 = Instant.now()

seconds = (Duration.between(start1, end1).toMillis() / 1000)

KeywordUtil.markPassed("⏱️ Order List terbuka dalam $seconds detik")

Mobile.tap(findTestObject('SBN/skip_sbn'), 0)

Mobile.delay(2, FailureHandling.STOP_ON_FAILURE)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Fixedincomesecondary7.PNG')

Mobile.swipe(500, 1500, 500, 500)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Fixedincomesecondary8.PNG')

Mobile.closeApplication()

