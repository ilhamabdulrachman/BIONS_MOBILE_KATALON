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
import java.util.ArrayList as ArrayList
import java.util.Map as Map

String clientID = '1B029'

String stockCode = 'ANTM'

BigDecimal orderPrice = new BigDecimal('59')

int lotAmount = 2

String side = 'B'

List<String> expectedStatuses = ['Open', 'Partial', 'Match (Executed)', 'Withdraw (Cancelled)', 'Amend', 'Reject', 'Pending New'
    , 'Hold Booking', 'Booked']

List<String> expectedBoardID = ['RG']

// --- Verifikasi Jam Bursa ---
boolean isMarketOpen = CustomKeywords.'com.utilities.TradingHours.isMarketOpen'()

if (isMarketOpen) {
    KeywordUtil.logInfo('Bursa sedang buka. Melanjutkan pengujian login...' // Menambahkan pengecekan market break (opsional, tapi disarankan)
        )
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
CustomKeywords.'com.utilities.AppHealth.verifyAppIsAlive'()
Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/LOGIN.PNG', FailureHandling.STOP_ON_FAILURE)

Mobile.setText(findTestObject('Login_firebase/User_id'), clientID, 0 // Menggunakan variabel clientID
    )

Mobile.setText(findTestObject('Login_firebase/Pw'), 'q', 0)

Mobile.setText(findTestObject('TEST_LOGIN/Pin2'), 'q12345', 0)

def start = Instant.now()

Mobile.tap(findTestObject('TEST_LOGIN/btn_'), 1)
CustomKeywords.'com.utilities.FreezeDetector.detectFrozenScreen'(
	5,
	2)

def end = Instant.now()

def seconds = Duration.between(start, end).toMillis() / 1000

KeywordUtil.logInfo("⏱️ Waktu login sampai dashboard: $seconds detik")

def now = ZonedDateTime.now(ZoneId.of('Asia/Jakarta'))

def fmt = DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss')

KeywordUtil.logInfo('Login successful at ' + now.format(fmt))

//Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Login.PNG')
Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/DASHBOARD.PNG', FailureHandling.STOP_ON_FAILURE)

Mobile.delay(5, FailureHandling.STOP_ON_FAILURE)


// ✅ SNAPSHOT PORTFOLIO AWAL
int beforeVolume = CustomKeywords.'com.utilities.OrderVerification.getStockVolumeFromPortfolio'(clientID, stockCode)

KeywordUtil.logInfo("📌 Snapshot portfolio BEFORE order | $stockCode = $beforeVolume")
CustomKeywords.'com.utilities.AppHealth.verifyAppIsAlive'()
CustomKeywords.'com.utilities.FreezeDetector.detectFrozenScreen'(
	5,
	2)
Mobile.tap(findTestObject('Transaksi/BUYSELL'), 1)

Mobile.tap(findTestObject('Transaksi/CHANGE'), 0)

Mobile.setText(findTestObject('Transaksi/Sell_/enter_stock_name'), 'ANTM', 0)

Mobile.tap(findTestObject('Transaksi/TAP_STOCK_NAME'), 0)
Mobile.delay(20, FailureHandling.STOP_ON_FAILURE)
boolean unhealthy =
    CustomKeywords.'com.utilities.FreezeDetector.detectFrozenOrCrashedScreenObserver'(5, 2)

if (unhealthy) {
    KeywordUtil.logInfo("⚠️ App terdeteksi freeze / crash (observer mode)")
} else {
    KeywordUtil.logInfo("✅ App normal setelah tap stock")
}
try {
    Mobile.takeScreenshot(
        '/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/app_health.PNG'
    )
    KeywordUtil.logInfo("📸 Screenshot app health berhasil")
} catch (Exception e) {
    KeywordUtil.logInfo("⚠️ Screenshot gagal (app crash / driver tidak siap)")
}

