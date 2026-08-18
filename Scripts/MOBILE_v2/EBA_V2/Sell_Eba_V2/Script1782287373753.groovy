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
import java.math.BigDecimal as BigDecimal

boolean isMarketOpen = CustomKeywords.'com.utilities.TradingHours.isMarketOpen'()

// --- 1. DEFINISI DATA TRANSAKSI ---
def clientID = '1B029'

def expectedBondCode = 'INDAH0105'

def expectedNominal = new BigDecimal('1000000')

def expectedPrice = new BigDecimal('101')

def expectedStatuses = ['CONFIRMED', 'PROCESSING', 'REJECT']

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

Mobile.setText(findTestObject('Login_firebase/User_id'), '1B029', 0)

Mobile.setText(findTestObject('Login_firebase/Pw'), 'q', 0)

Mobile.setText(findTestObject('Login_firebase/Pin'), 'q12345', 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Login0.PNG')

start = Instant.now()

Mobile.tap(findTestObject('Login_V2/login'), 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Login1.PNG')

start = Instant.now()

Mobile.tap(findTestObject('Login_V2/login'), 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Login1.PNG')

TcpClient client = new TcpClient()
client.connect('REDACTED_INTERNAL_IP', 62229)   // FEED_SERVER_1
client.sendMessage('{ "action":"login", "user":"1B029", "password":"q" }')
client.listen(3)

if (client.hasResponse()) {
    KeywordUtil.logInfo("📩 Response TCP diterima: " + client.getAllResponses())
} else {
    KeywordUtil.logInfo("ℹ️ Tidak ada response dari TCP feed server (server bersifat one-way/broadcast)")
}
client.close()

end = Instant.now()

double seconds = (Duration.between(start, end).toMillis() / 1000.0)

KeywordUtil.logInfo("⏱️ Waktu login sampai dashboard: $seconds detik")

def now = ZonedDateTime.now(ZoneId.of('Asia/Jakarta'))

def fmt = DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss')

KeywordUtil.logInfo('Login successful at ' + now.format(fmt))

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Login_Biometric.PNG')

//Mobile.tap(findTestObject('Login_firebase/Not_now'), 0)
//
//Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Login_V2.PNG')
Mobile.tap(findTestObject('Eba_V2/Eba_V2'), 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/EBA_V2_11.PNG')

Mobile.tap(findTestObject('Eba_V2/indah_105'), 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/EBA_V2_12.PNG')

Mobile.tap(findTestObject('Eba_V2/Sell_eba_v2'), 0)

Mobile.delay(3, FailureHandling.STOP_ON_FAILURE)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/EBA_V2_13.PNG')

Mobile.tap(findTestObject('Eba_V2/button_sell_eba_v2'), 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/EBA_V2_15.PNG')

Mobile.tap(findTestObject('Eba_V2/Checkbox_sell'), 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/EBA_V2_16.PNG')

Mobile.tap(findTestObject('Eba_V2/confirm_submit_sell'), 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/EBA_V2_17.PNG')

Mobile.tap(findTestObject('Eba_V2/View_orderlist_sell'), 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/EBA_V2_18.PNG')

KeywordUtil.logInfo('Memulai Verifikasi Database untuk TB_FO_BONDTRANSACTION...')

boolean bondResult = CustomKeywords.'com.utilities.OrderVerification.verifyLatestBondTransaction'(clientID, expectedBondCode, 
    expectedNominal, expectedPrice, expectedStatuses, null, null)

if (bondResult) {
    KeywordUtil.markPassed("✅ Verifikasi DB Bond Transaksi Berhasil: Data order $expectedBondCode ditemukan di database dengan status yang diharapkan.")
} else {
    KeywordUtil.markFailed('❌ Verifikasi DB Bond Transaksi GAGAL. Cek log error Custom Keyword.')
}

Mobile.closeApplication()

