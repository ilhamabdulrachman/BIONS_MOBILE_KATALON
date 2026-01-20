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
def clientID = '1B029'
def expectedBondCode = 'KTW5'
def expectedNominal = new BigDecimal('5000000')
// Asumsi Price: Harap ganti 100 dengan harga aktual yang Anda masukkan atau baca dari UI
def expectedPrice = new BigDecimal('110')

def expectedStatuses = ['CONFIRMED', 'PROCCESING' , 'REJECT']
def expectedEstampDuty    = null
def expectedTotalPayment = null


//NetworkChecker.verifyInternetConnection()
//Mobile.startApplication('/Users/bionsrevamp/Downloads/app-development-profile 1 (1).apk', true)
String applicationID = 'id.bions.bnis.android.v2'

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

Mobile.setText(findTestObject('TEST_LOGIN/Pin2'), 'q12345', 0)

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

Mobile.tap(findTestObject('SBN/Tap_Fixed_Income'), 1)

end = Instant.now()

seconds = (Duration.between(start, end).toMillis() / 1000)

KeywordUtil.logInfo("⏱️ Waktu login sampai dashboard: $seconds detik")

Mobile.delay(2, FailureHandling.STOP_ON_FAILURE)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Fixedincomesecondary.PNG')

Mobile.tap(findTestObject('Corp_Bond/Corp_Bond'), 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Corp_Bond.PNG')

Mobile.tap(findTestObject('Corp_Bond/Secondary_CorpBond'), 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Corp_Bond1.PNG')

Mobile.delay(2, FailureHandling.STOP_ON_FAILURE)

Mobile.tap(findTestObject('Corp_Bond/Ktw_5'), 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Corp_Bond2.PNG')

Mobile.tap(findTestObject('Corp_Bond/Buy_Secondary'), 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Corp_Bond3.PNG')

Mobile.setText(findTestObject('Corp_Bond/Buy_Amount'), '5000000', 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Corp_Bond4.PNG')

//boolean allowed = CustomKeywords.'com.utilities.TradingHours.isCorpBondnSecondaryAllowed'()
//
//if (!allowed) {
//	KeywordUtil.markFailedAndStop("❌ Order CORP BOND Secondary hanya bisa di Sesi 1 .")
//}

Mobile.tap(findTestObject('Corp_Bond/Klik_Buy_Secondary'), 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Corp_Bond5.PNG')

Mobile.tap(findTestObject('Corp_Bond/Confirm_Submint'), 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Corp_Bond6.PNG')

// --- 6. DATABASE VERIFICATION (CRITICAL STEP) ---
KeywordUtil.logInfo("Memulai Verifikasi Database untuk TB_FO_BONDTRANSACTION...")


boolean bondResult = CustomKeywords.'com.utilities.OrderVerification.verifyLatestBondTransaction'(
	clientID,
	expectedBondCode,
	expectedNominal,
	expectedPrice,
	expectedStatuses,
	expectedEstampDuty,
	expectedTotalPayment,
)

if (bondResult) {
	KeywordUtil.markPassed("✅ Verifikasi DB Bond Transaksi Berhasil: Data order ${expectedBondCode} ditemukan di database dengan status yang diharapkan.")
} else {
	KeywordUtil.markFailed("❌ Verifikasi DB Bond Transaksi GAGAL. Cek log error Custom Keyword.")
}

Mobile.tap(findTestObject('Corp_Bond/View_Order_List'), 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Corp_Bond7.PNG')

Mobile.delay(7, FailureHandling.STOP_ON_FAILURE)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Corp_Bond8.PNG')

Mobile.closeApplication()
