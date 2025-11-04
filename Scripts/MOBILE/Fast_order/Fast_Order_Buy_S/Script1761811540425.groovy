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
import com.utilities.OrderVerification as OrderVerification
import java.math.BigDecimal as BigDecimal

String clientID = '1B029'

String stockCode = 'KICI'

BigDecimal orderPrice = new BigDecimal('412')

int lotAmount = 1

String side = 'B'

List<String> expectedStatuses = ['Open', 'Partial', 'Match (Executed)', 'Withdraw (Cancelled)', 'Amend', 'Reject', 'Pending New'
    , 'Hold Booking', 'Booked']

List<String> expectedBoardID = ['RG']

boolean isMarketOpen = CustomKeywords.'com.utilities.TradingHours.isMarketOpen'()

if (isMarketOpen) {
    KeywordUtil.logInfo('Bursa sedang buka. Melanjutkan pengujian login...')
} else {
    boolean isMarketBreak = CustomKeywords.'com.utilities.TradingHours.isMarketBreak'()

    if (isMarketBreak) {
        KeywordUtil.markFailed('Tes gagal. Bursa sedang istirahat.', FailureHandling.STOP_ON_FAILURE)
    } else {
        KeywordUtil.markFailed('Tes gagal. Bursa sedang tutup.', FailureHandling.STOP_ON_FAILURE)
    }
}

String applicationID = 'id.bions.bnis.android.v2'

try {
    Mobile.startExistingApplication(applicationID, FailureHandling.STOP_ON_FAILURE)

    KeywordUtil.logInfo("✅ Aplikasi dengan ID '$applicationID' berhasil diluncurkan.")
}
catch (Exception e) {
    KeywordUtil.markFailed('❌ Gagal meluncurkan aplikasi. Pastikan aplikasi sudah terinstal di perangkat. Error: ' + e.getMessage(), 
        FailureHandling.STOP_ON_FAILURE)
} 

//def elemenDashboard = findTestObject('TEST_LOGIN/SKIP_QUIK_TOUR')
//NetworkChecker.verifyInternetConnection()
//Mobile.startApplication('/Users/bionsrevamp/Downloads/app-production-profile.apk', true)
Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/LOGIN.PNG', FailureHandling.STOP_ON_FAILURE)

//NetworkChecker.verifyInternetConnection()
//Mobile.tap(findTestObject('TEST_LOGIN/skip_onboarding'), 0)
Mobile.setText(findTestObject('Login_firebase/User_id'), '1B029', 0)

Mobile.setText(findTestObject('Login_firebase/Pw'), 'q', 0)

Mobile.setText(findTestObject('Login_firebase/Pin'), 'q12345', 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Login0.PNG')

def start1 = Instant.now()

Mobile.tap(findTestObject('TEST_LOGIN/btn_'), 1)

def end1 = Instant.now()

def seconds1 = Duration.between(start1, end1).toMillis() / 1000

KeywordUtil.logInfo("⏱️ Waktu login sampai dashboard: $seconds1 detik")

def now = ZonedDateTime.now(ZoneId.of('Asia/Jakarta'))

def fmt = DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss')

KeywordUtil.logInfo('Login successful at ' + now.format(fmt))

//NetworkChecker.verifyInternetConnection()
Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Login1.PNG')

TcpClient client = new TcpClient()

client.connect('192.168.19.61', 62229)

// Kirim login
client.sendMessage('{ "action":"login", "user":"1B029", "password":"q" }')

// Listen 5 detik untuk capture response login
client.listen(5)

// 🔌 Tutup koneksi
client.close()

//Mobile.tap(findTestObject('TEST_LOGIN/SKIP_QUIK_TOUR'), 0)
Mobile.delay(1, FailureHandling.STOP_ON_FAILURE)

Mobile.tap(findTestObject('Fast_Order_Buy/Klik_more'), 1)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Fastorder11.PNG')

Mobile.tap(findTestObject('Fast_Order_Buy/Klik_fastorder'), 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Fastorder12.PNG')

Mobile.tap(findTestObject('Fast_Order_Buy/change_fast_order1'), 0)

Mobile.setText(findTestObject('Fast_Order_Buy/enter_stock_name'), 'KICI', 0)

Mobile.tap(findTestObject('Fast_Order_Buy/Tap_stock_fast'), 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Fastorder13.PNG')

Mobile.tap(findTestObject('Fast_Order_Buy/ACC_TYPE'), 0)

Mobile.tap(findTestObject('Fast_Order_Buy/Syariah_fast_order'), 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Fastorder14.PNG')

Mobile.delay(8, FailureHandling.STOP_ON_FAILURE)

Mobile.tap(findTestObject('Fast_Order_Buy/checkbox fastorder1'), 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Fastorder15.PNG')

Mobile.delay(3, FailureHandling.STOP_ON_FAILURE)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Fastorder16.PNG')

Mobile.tap(findTestObject('Fast_Order_Buy/confirm and submit fast order'), 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Fastorder17.PNG')

Mobile.swipe(500, 1500, 500, 500)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Fastorder18.PNG')

Mobile.swipe(500, 1500, 500, 500)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Fastorder19.PNG')

KeywordUtil.logInfo("Memulai verifikasi database untuk order client ID $clientID...")

// *** Panggilan Verifikasi TB_FO_ORDER ***
boolean dbVerificationResult = CustomKeywords.'com.utilities.OrderVerification.verifyLatestRegularOrder'(clientID, stockCode, 
    lotAmount, orderPrice, expectedStatuses, side, expectedBoardID)

if (dbVerificationResult) {
    KeywordUtil.logInfo('✅ STATUS: Transaksi order berhasil dan SINKRON dengan Database Oracle.')
} else {
    KeywordUtil.logError('❌ STATUS: Ketidaksesuaian data order ditemukan di database.')
}

Mobile.closeApplication()

