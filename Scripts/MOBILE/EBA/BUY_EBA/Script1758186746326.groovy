import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords as Mobile
import com.kms.katalon.core.model.FailureHandling as FailureHandling
import com.kms.katalon.core.util.KeywordUtil as KeywordUtil
import java.time.ZonedDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.time.Duration
import com.utilities.TcpClient as TcpClient
import java.math.BigDecimal

// --- 1. DEFINISI DATA TRANSAKSI ---
def clientID = '1B029'
def expectedBondCode = 'INDAH0105'
def expectedNominal = new BigDecimal('1000000')
// Asumsi Price: Harap ganti 100 dengan harga aktual yang Anda masukkan atau baca dari UI
def expectedPrice = new BigDecimal('101')

def expectedStatuses = ['CONFIRMED', 'PROCCESING' , 'REJECT'] 

// --- 2. SETUP & START APLIKASI ---
String applicationID = 'id.bions.bnis.android.v2'

try {
	Mobile.startExistingApplication(applicationID, FailureHandling.STOP_ON_FAILURE)

	KeywordUtil.logInfo("✅ Aplikasi dengan ID '$applicationID' berhasil diluncurkan.")
}
catch (Exception e) {
	KeywordUtil.markFailed('❌ Gagal meluncurkan aplikasi. Pastikan aplikasi sudah terinstal di perangkat. Error: ' + e.getMessage(),
		FailureHandling.STOP_ON_FAILURE)
}
//Mobile.startApplication('/Users/bionsrevamp/Downloads/app-development-profile 1 (1).apk', true)
Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/LOGIN.PNG', FailureHandling.STOP_ON_FAILURE)

//Mobile.tap(findTestObject('TEST_LOGIN/skip_onboarding'), 0)

// --- 3. PROSES LOGIN ---
Mobile.setText(findTestObject('Login_firebase/User_id'), clientID, 0)
Mobile.setText(findTestObject('Login_firebase/Pw'), 'q', 0)
Mobile.setText(findTestObject('Login_firebase/Pin'), 'q12345', 0)
Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Login0.PNG')

Instant startLogin = Instant.now()
Mobile.tap(findTestObject('TEST_LOGIN/btn_'), 0)

def now = ZonedDateTime.now(ZoneId.of("Asia/Jakarta"))
def fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
KeywordUtil.logInfo("Login successful at " + now.format(fmt))

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Login1.PNG')
Mobile.delay(2, FailureHandling.STOP_ON_FAILURE)

// --- 4. VERIFIKASI DENGAN TCP CLIENT (KEEP AS IS) ---
TcpClient client = new TcpClient()
client.connect('192.168.19.61', 62229)
client.sendMessage("{ \"action\":\"login\", \"user\":\"${clientID}\", \"password\":\"q\" }")
client.listen(5)
client.close()

Instant endLogin = Instant.now()
long secondsLogin = Duration.between(startLogin, endLogin).toMillis() / 1000
KeywordUtil.logInfo("⏱️ Waktu login sampai dashboard: ${secondsLogin} detik")

Mobile.delay(2, FailureHandling.STOP_ON_FAILURE)
//Mobile.tap(findTestObject('TEST_LOGIN/SKIP_QUIK_TOUR'), 0)

// --- 5. NAVIGASI DAN SUBMIT FIXED INCOME ORDER ---
Mobile.tap(findTestObject('SBN/Tap_Fixed_Income'), 1)
Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Fixedincomeeba.PNG')
Mobile.swipe(500, 1500, 500, 500)
Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Fixedincomeeba1.PNG')
Mobile.delay(2, FailureHandling.STOP_ON_FAILURE)

// Pilih Bond, Tap Buy, Input Nominal, Konfirmasi
Mobile.tap(findTestObject('EBA_BUY/INDAH0105'), 0)
Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Fixedincomeeba2.PNG')
Mobile.tap(findTestObject('EBA_BUY/BUTTON_BUY'), 0)
Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Fixedincomeeba3.PNG')
Mobile.setText(findTestObject('EBA_BUY/AMOUNT'), expectedNominal.toString(), 0) // Menggunakan variabel Nominal
Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Fixedincomeeba4.PNG')
Mobile.tap(findTestObject('EBA_BUY/Buy_Eba'), 0)
Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Fixedincomeeba5.PNG')
Mobile.tap(findTestObject('EBA_BUY/Tick_buy_eba'), 0)
Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Fixedincomeeba6.PNG')
Mobile.tap(findTestObject('EBA_BUY/confirm_submit'), 0)

KeywordUtil.logInfo("Order Sent at " + ZonedDateTime.now(ZoneId.of("Asia/Jakarta")).format(fmt))
Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Fixedincomeeba7.PNG')

// --- 6. DATABASE VERIFICATION (CRITICAL STEP) ---
KeywordUtil.logInfo("Memulai Verifikasi Database untuk TB_FO_BONDTRANSACTION...")


boolean bondResult = CustomKeywords.'com.utilities.OrderVerification.verifyLatestBondTransaction'(
	clientID,
	expectedBondCode,
	expectedNominal,
	expectedPrice,
	expectedStatuses,
)

if (bondResult) {
	KeywordUtil.markPassed("✅ Verifikasi DB Bond Transaksi Berhasil: Data order ${expectedBondCode} ditemukan di database dengan status yang diharapkan.")
} else {
	KeywordUtil.markFailed("❌ Verifikasi DB Bond Transaksi GAGAL. Cek log error Custom Keyword.")
}


// --- 7. VERIFIKASI UI ORDER LIST ---
Instant startView = Instant.now()
Mobile.tap(findTestObject('EBA_BUY/view_order_lisf'), 0)
Instant endView = Instant.now()
long secondsView = Duration.between(startView, endView).toMillis() / 1000
KeywordUtil.markPassed("⏱️ Order List terbuka dalam ${secondsView} detik")

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Fixedincomeeba8.PNG')
//Mobile.tap(findTestObject('EBA_BUY/skip_tour_eba'), 0)
Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Fixedincomeeba9.PNG')
Mobile.delay(2, FailureHandling.STOP_ON_FAILURE)
Mobile.swipe(500, 1500, 500, 500)
Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Fixedincomeeba10.PNG')
Mobile.closeApplication()
