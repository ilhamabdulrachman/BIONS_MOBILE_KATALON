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
import java.time.ZonedDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.time.Duration
import com.utilities.TradingHours


boolean isMarketOpen = CustomKeywords.'com.utilities.TradingHours.isMarketOpen'()
if (isMarketOpen) {
	KeywordUtil.logInfo("Bursa sedang buka. Melanjutkan pengujian login...")
} else {
	// Jika bursa tutup, hentikan tes
	KeywordUtil.markFailed("Tes gagal. Bursa sedang tutup.", FailureHandling.STOP_ON_FAILURE)
}

Mobile.startApplication('/Users/bionsrevamp/Downloads/app-production-profile.apk', false)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/LOGIN.PNG', FailureHandling.STOP_ON_FAILURE)

Mobile.tap(findTestObject('TEST_LOGIN/skip_onboarding'), 0)

Mobile.setText(findTestObject('Login_firebase/User_id'), '23AA50456', 0)

Mobile.setText(findTestObject('Login_firebase/Pw'), 'kittiw222', 0)

Mobile.setText(findTestObject('Login_firebase/Pin'), 'kittiw333', 0)

Instant start = Instant.now()

Mobile.tap(findTestObject('TEST_LOGIN/btn_'), 1)

Instant end = Instant.now()

long seconds = Duration.between(start, end).toMillis() / 1000

KeywordUtil.logInfo("⏱️ Waktu login sampai dashboard: ${seconds} detik")

def now = ZonedDateTime.now(ZoneId.of("Asia/Jakarta"))
def fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

KeywordUtil.logInfo("Login successful at " + now.format(fmt))

//Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Login.PNG')
Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/DASHBOARD.PNG', FailureHandling.STOP_ON_FAILURE)

Mobile.tap(findTestObject('TEST_LOGIN/SKIP_QUIK_TOUR'), 1)

Mobile.tap(findTestObject('Transaksi/button_buy_sell'), 1)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Basicorder.PNG')

Mobile.tap(findTestObject('Transaksi/Skip_basic_order'), 1)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Basicorder1.PNG')

Instant start = Instant.now()

Mobile.tap(findTestObject('Transaksi/button_buy'), 1)
Instant end = Instant.now()

long seconds = Duration.between(start, end).toMillis() / 1000

KeywordUtil.logInfo("⏱️ Waktu masuk ke halaman form buy : ${seconds} detik")

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Basicorder2.PNG')

Mobile.tap(findTestObject('Transaksi/confirm_submit_buy'), 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Basicorder3.PNG')

def client = new TcpClient()

client.connect('trade.bions.id', 62229)

// Kirim login
client.sendMessage('{ "action":"login", "user":"23AA50456", "password":"kittiw222" }')

// Listen 5 detik untuk capture response login
client.listen(5)

// Kirim subscribe order
client.sendMessage('{ "action":"subscribe", "channel":"order", "user":"23AA50456" }')

// Listen 10 detik untuk capture response order
client.listen(10)

// Tutup koneksi
client.close()

Mobile.tap(findTestObject('Transaksi/view_order_list'), 1)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Orderlist.PNG')

Mobile.tap(findTestObject('Transaksi/Skip_quick_tour_orderlist'), 1)

Mobile.closeApplication()

