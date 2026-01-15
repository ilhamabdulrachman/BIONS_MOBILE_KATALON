import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords as Mobile
import static com.kms.katalon.core.testdata.TestDataFactory.findTestData
import com.kms.katalon.core.model.FailureHandling as FailureHandling
import internal.GlobalVariable as GlobalVariable
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

// ====================================================================
// --- 1. DEKLARASI VARIABEL UNTUK DATA BINDING (WAJIB DIISI) ---
// Variabel ini akan diisi dari Data File di Test Suite
// ====================================================================
def UI_Username
def Password
def PIN
//def Application_ID = 'id.bions.bnis.android.v2' // Nilai default untuk Application ID
// VARIABEL BARU KRITIS UNTUK START APLIKASI
//def App_Activity = 'com.bions.bnis.android.v2.ui.activities.SplashScreenActivity' // <--- GANTI INI JIKA GAGAL

// --- CEK JAM BURSA ---
//boolean isMarketOpen = CustomKeywords.'com.utilities.TradingHours.isMarketOpen'()
//
//if (isMarketOpen) {
//    KeywordUtil.logInfo('✅ Bursa sedang buka. Melanjutkan pengujian...')
//} else {
//    boolean isMarketBreak = CustomKeywords.'com.utilities.TradingHours.isMarketBreak'()
//
//    if (isMarketBreak) {
//        KeywordUtil.markFailed('❌ Tes gagal. Bursa sedang istirahat.', FailureHandling.STOP_ON_FAILURE)
//    } else {
//        KeywordUtil.markFailed('❌ Tes gagal. Bursa sedang tutup.', FailureHandling.STOP_ON_FAILURE)
//    }
//}

// Definisikan objek dashboard untuk verifikasi
//def elemenDashboard = findTestObject('TEST_LOGIN/stock')

// --- 2. START APPLICATION (Menggunakan Variabel Aplikasi ID dan Activity) ---
// CATATAN PENTING: Dengan menyertakan Activity Name, kita memastikan Appium meluncurkan dan menunggu Activity yang benar.
String applicationID = 'id.bions.bnis.android.v2'

try {
    Mobile.startApplication(Application_ID, App_Activity, FailureHandling.STOP_ON_FAILURE)

    KeywordUtil.logInfo("✅ Aplikasi dengan ID '$Application_ID' berhasil diluncurkan dengan Activity: '$App_Activity'.")
}
catch (Exception e) {
    KeywordUtil.markFailed('❌ Gagal meluncurkan aplikasi. Error: ' + e.getMessage(), FailureHandling.STOP_ON_FAILURE)
}

// --- 3. MOBILE LOGIN UI (Menggunakan Variabel Login) ---
// Mobile.tap(findTestObject('TEST_LOGIN/skip_onboarding'), 0) // Uncomment jika onboarding aktif

Mobile.setText(findTestObject('Login_firebase/User_id'), UI_Username, 0)
Mobile.setText(findTestObject('Login_firebase/Pw'), Password, 0)
Mobile.setText(findTestObject('Login_firebase/Pin'), PIN, 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Login_PreTap.PNG')

Instant start = Instant.now()

Mobile.tap(findTestObject('TEST_LOGIN/btn_'), 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Login_PostTap.PNG')

// --- 4. NETWORK LOGIN (TCP/SOCKET) ---
TcpClient client = new TcpClient()

// Menggunakan mock server. Ubah ke 'trade.bions.id' jika menguji live feed.
client.connect('mock.bions.xyz', 62229) 

// Kirim login menggunakan Variabel UI (Asumsi kredensial UI dan Network sama)
def networkPayload = "{ \"action\":\"login\", \"user\":\"" + UI_Username + "\", \"password\":\"" + Password + "\" }"

KeywordUtil.logInfo("Sending TCP Login Payload: " + networkPayload)
client.sendMessage(networkPayload)

// Listen 5 detik untuk capture response login
String response = client.listen(5)
KeywordUtil.logInfo("Received TCP Response: " + response)

// --- VERIFIKASI (ASSERTION) TCP RESPONSE ---
// Verifikasi ini memastikan koneksi ke server feed data berhasil dan terautentikasi
try {
    // Gunakan JsonSlurper untuk mengurai respons menjadi objek Groovy/Java
    def jsonResponse = new JsonSlurper().parseText(response)

    // ASUMSI: Server merespons dengan JSON yang memiliki status 'OK'
    // Ubah 'OK' sesuai dengan respons sukses yang sebenarnya dari server Anda.
    assert jsonResponse.status == 'OK' : "❌ TCP Login Gagal. Status: " + jsonResponse.status
    
    KeywordUtil.logInfo("✅ TCP Login ke feed server berhasil.")

} catch (groovy.json.JsonException e) {
    // Tangani jika respons BUKAN JSON yang valid (atau server tidak merespons)
    KeywordUtil.markFailed('❌ Respons TCP bukan JSON yang valid atau kosong. Response: ' + response, FailureHandling.STOP_ON_FAILURE)
} catch (AssertionError e) {
    // Tangani jika status dalam JSON tidak sesuai harapan
    KeywordUtil.markFailed('❌ Verifikasi status TCP gagal: ' + e.getMessage(), FailureHandling.STOP_ON_FAILURE)
}

// 🔌 Tutup koneksi
client.close()

Instant end = Instant.now()

// Hitung durasi dan tampilkan
long loginDurationMillis = Duration.between(start, end).toMillis()
def loginDurationSeconds = String.format("%.3f", (loginDurationMillis / 1000.0))

KeywordUtil.logInfo("⏱️ Waktu login UI dan Network: $loginDurationSeconds detik")

def now = ZonedDateTime.now(ZoneId.of('Asia/Jakarta'))
def fmt = DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss')
KeywordUtil.logInfo('Login successful at ' + now.format(fmt))

// --- 5. POST LOGIN NAVIGATION & VERIFICATION ---
Mobile.delay(3, FailureHandling.STOP_ON_FAILURE) 
// Mobile.tap(findTestObject('TEST_LOGIN/SKIP_QUIK_TOUR'), 0) // Jika elemen ini muncul

Mobile.swipe(500, 1500, 500, 500)
Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Dashboard1.PNG')

// ... (Swipes lainnya) ...

Mobile.swipe(500, 1500, 500, 500)
Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Dashboard4.PNG')

// --- 6. TIME TO PORTFOLIO ---
Instant start1 = Instant.now()
Mobile.tap(findTestObject('NAVBAR/portofolio'), 0)
Instant end1 = Instant.now()

long portfolioDurationMillis = Duration.between(start1, end1).toMillis()
def portfolioDurationSeconds = String.format("%.3f", (portfolioDurationMillis / 1000.0))

KeywordUtil.logInfo("⏱️ Waktu sampai Portofolio: ${portfolioDurationSeconds} detik")

Mobile.closeApplication()