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

// Catatan: Nilai ini harus SAMA dengan data yang diinputkan/default di UI
String clientID = '1B029' // Ditambahkan: ID Klien

String stockCode = 'APLN' // Ditambahkan: Sesuaikan dengan saham yang di-order

BigDecimal orderPrice = new BigDecimal('176')

int lotAmount = 1 // Ditambahkan: Sesuaikan dengan lot yang di-order

String side = 'S'

List<String> expectedStatuses = ['Open', 'Partial', 'Match (Executed)', 'Withdraw (Cancelled)', 'Amend', 'Reject', 'Pending New'
    , 'Hold Booking', 'Booked']

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

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/LOGIN.PNG', FailureHandling.STOP_ON_FAILURE)

Mobile.setText(findTestObject('Login_firebase/User_id'), clientID, 0 // Menggunakan variabel clientID
    )

Mobile.setText(findTestObject('Login_firebase/Pw'), 'q', 0)

Mobile.setText(findTestObject('Login_firebase/Pin'), 'q12345', 0)

def start = Instant.now()

Mobile.tap(findTestObject('TEST_LOGIN/btn_'), 1)

def end = Instant.now()

def seconds = Duration.between(start, end).toMillis() / 1000

KeywordUtil.logInfo("⏱️ Waktu login sampai dashboard: $seconds detik")

def now = ZonedDateTime.now(ZoneId.of('Asia/Jakarta'))

def fmt = DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss')

KeywordUtil.logInfo('Login successful at ' + now.format(fmt))

//Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Login.PNG')
Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/DASHBOARD.PNG', FailureHandling.STOP_ON_FAILURE)

Mobile.delay(5, FailureHandling.STOP_ON_FAILURE)

Mobile.tap(findTestObject('Transaksi/BUYSELL'), 1)

Mobile.tap(findTestObject('Transaksi/CHANGE'), 0)

Mobile.setText(findTestObject('Transaksi/STOCK_NAME'), 'APLN', 0)

Mobile.tap(findTestObject('Transaksi/TAP_STOCK_NAME'), 0)

Mobile.tap(findTestObject('Transaksi/Sell_/TAB_SELL'), 1)

Mobile.delay(8, FailureHandling.STOP_ON_FAILURE)

//Mobile.setText(findTestObject('Transaksi/input_price_'), '171', 0)
Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/ORDERS1.PNG')

//Mobile.tap(findTestObject('Transaksi/send_order_booking'), 0)
start = Instant.now()

Mobile.tap(findTestObject('Transaksi/button_buy'), 1)

end = Instant.now()

seconds = (Duration.between(start, end).toMillis() / 1000)

KeywordUtil.logInfo("⏱️ Waktu masuk ke halaman form buy : $seconds detik")

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/ORDERS3.PNG')

Mobile.tap(findTestObject('Transaksi/confirm_submit_buy'), 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/ORDERS4.PNG')

def client = new TcpClient()

client.connect('192.168.19.61', 62229)

// Kirim login - Menggunakan clientID dari variabel (Sintaks diperbaiki)
client.sendMessage("{\"action\":\"login\", \"user\":\"${clientID}\", \"password\":\"q12345\"}")

// Listen 5 detik untuk capture response login
client.listen(5)

// Kirim subscribe order - Menggunakan clientID dari variabel (Sintaks diperbaiki)
client.sendMessage("{\"action\":\"subscribe\", \"channel\":\"order\", \"user\":\"${clientID}\"}")

// Listen 10 detik untuk capture response order
client.listen(10)

// Tutup koneksi
client.close()

start1 = Instant.now()

Mobile.tap(findTestObject('Transaksi/view_order_list'), 1)

end1 = Instant.now()

seconds = (Duration.between(start1, end1).toMillis() / 1000)

KeywordUtil.markPassed("⏱️ Order List terbuka dalam $seconds detik")

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Orderlist3.PNG')

Mobile.delay(2, FailureHandling.STOP_ON_FAILURE)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/ORDERS5.PNG')

Mobile.swipe(500, 1500, 500, 500)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/ORDERS6.PNG')

KeywordUtil.logInfo("Memulai verifikasi database untuk order client ID $clientID...")

// *** Panggilan Verifikasi TB_FO_ORDER ***
boolean dbVerificationResult = CustomKeywords.'com.utilities.OrderVerification.verifyLatestRegularOrder'(clientID, stockCode, 
    lotAmount, orderPrice, expectedStatuses, side)

if (dbVerificationResult) {
    KeywordUtil.logInfo('✅ STATUS: Transaksi order berhasil dan SINKRON dengan Database Oracle.')
} else {
    KeywordUtil.logError('❌ STATUS: Ketidaksesuaian data order ditemukan di database.')
}

Mobile.closeApplication()

