import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords as Mobile
import com.kms.katalon.core.model.FailureHandling as FailureHandling
import com.kms.katalon.core.util.KeywordUtil as KeywordUtil
import internal.GlobalVariable as GlobalVariable
import java.time.ZonedDateTime as ZonedDateTime
import java.time.ZoneId as ZoneId
import java.time.format.DateTimeFormatter as DateTimeFormatter
import java.time.Instant as Instant
import java.time.Duration as Duration
import com.utilities.BionsSocketClient as BionsSocketClient
import com.utilities.NetworkDiagnostic

// ============================================================
// ⚠️ PERINGATAN: TEST CASE INI UNTUK ENVIRONMENT DEMO SAJA
// Menggunakan Global Variable *Dev (G_userIdDev, dst).
// ============================================================

// ============================================================
// KONFIGURASI
// ============================================================
String applicationID = 'id.bions.bnis.android.new_bions_revamp'
String screenshotBasePath = '/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login'

String userId = GlobalVariable.G_userIdDev
String password = GlobalVariable.G_passwordDev
String pin = GlobalVariable.G_pindev
String host = GlobalVariable.G_tradinghostDev
int feedPort = GlobalVariable.G_feedport.toInteger()
int tradingPort = GlobalVariable.G_tradingport.toInteger()
String clientIp = GlobalVariable.G_clientip

// ===== PARAMETER ORDER =====
String stockCode = 'BBNI'      // TANPA suffix board
String boardCode = 'RG'
String orderSide = '1'         // '1' = Buy, '2' = Sell
int orderLot = 1
BigDecimal orderPrice = 3780.0

int totalChecks = 0
int passedChecks = 0

def verify = { boolean condition, String description ->
    totalChecks++
    if (condition) {
        passedChecks++
        KeywordUtil.logInfo("✅ PASS: ${description}")
    } else {
        KeywordUtil.markWarning("❌ FAIL: ${description}")
    }
}

// ============================================================
// STEP 1: LAUNCH APPLICATION
// ============================================================
try {
    Mobile.startExistingApplication(applicationID, FailureHandling.STOP_ON_FAILURE)
    KeywordUtil.logInfo("Aplikasi dengan ID '${applicationID}' berhasil diluncurkan.")
} catch (Exception e) {
    KeywordUtil.markFailed('Gagal meluncurkan aplikasi. Pastikan aplikasi sudah terinstal di perangkat. Error: ' + e.getMessage())
    return
}

// ============================================================
// STEP 2: LOGIN VIA UI MOBILE
// ============================================================
Mobile.setText(findTestObject('Login_firebase/User_id'), userId, 0)
Mobile.setText(findTestObject('Login_firebase/Pw'), password, 0)
Mobile.setText(findTestObject('Login_firebase/Pin'), pin, 0)
Mobile.takeScreenshot("${screenshotBasePath}/Login0.PNG")

Instant start = Instant.now()
Mobile.tap(findTestObject('Login_V2/button_login'), 0)
Mobile.takeScreenshot("${screenshotBasePath}/Login1.PNG")
Instant end = Instant.now()

double loginSeconds = Duration.between(start, end).toMillis() / 1000.0
KeywordUtil.logInfo("Waktu login sampai dashboard: ${loginSeconds} detik")

boolean loginSuccess = Mobile.verifyElementExist(
        findTestObject('Login_V2/button_notnow'),
        10,
        FailureHandling.OPTIONAL
)

if (!loginSuccess) {
    Mobile.takeScreenshot("${screenshotBasePath}/Login_FAILED.PNG")
    Map diagnostic = NetworkDiagnostic.runDiagnostic(host, feedPort)
    KeywordUtil.markFailed('❌ Login GAGAL - biometric prompt tidak muncul dalam 10 detik. ' +
            "Diagnosa: ${diagnostic.diagnosis}")
    return
}

KeywordUtil.logInfo('✅ Login berhasil terverifikasi - biometric prompt terdeteksi.')

def now = ZonedDateTime.now(ZoneId.of('Asia/Jakarta'))
def fmt = DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss')
KeywordUtil.logInfo('Login successful at ' + now.format(fmt))

Mobile.takeScreenshot("${screenshotBasePath}/Login_Biometric.PNG")
Mobile.tap(findTestObject('Login_V2/button_notnow'), 0)

Mobile.tap(findTestObject('NavBar_Scalability/trade_'), 0)
Mobile.takeScreenshot("${screenshotBasePath}/Trade_V2.PNG")

//Mobile.swipe(500, 1500, 500, 500)

Mobile.takeScreenshot("${screenshotBasePath}/Trade_V1.PNG")

// ============================================================
// STEP 2B: ORDER VIA UI (OPSIONAL - kalau mau tetap coba jalur UI juga)
// Dikomentari secara default karena fokus test ini adalah SOCKET placeOrder.
// Uncomment kalau ingin membandingkan hasil UI vs socket.
// ============================================================
// Mobile.tap(findTestObject('BUY/Price'), 0)
// Mobile.setText(findTestObject('BUY/Price'), '3780', 0)
// Mobile.tap(findTestObject('BUY/LOT'), 0)
// Mobile.setText(findTestObject('BUY/LOT'), '1', 0)
// Mobile.tap(findTestObject('BUY/buy'), 0)
// Mobile.tap(findTestObject('BUY/Confirm'), 0)

Mobile.closeApplication()

// CATATAN: Semua interaksi UI SUDAH SELESAI di titik ini. Socket login
// di bawah kemungkinan membuat sesi UI ter-logout (kebijakan single-session
// server) - ini sudah diperhitungkan dalam desain test case ini.

// ============================================================
// STEP 3: SOCKET TRADING LOGIN
// ============================================================
BionsSocketClient tradingClient = new BionsSocketClient()

try {
    Map tradingResult = tradingClient.loginTrading(host, tradingPort, userId, password, pin, clientIp)
    verify(tradingResult.success == true, 'Trading Login berhasil')
    KeywordUtil.logInfo("Trading Gateway: ${tradingResult.tag58Value}")
} catch (Exception e) {
    KeywordUtil.logInfo("❌ Trading Login gagal: ${e.message}")
    Map diagnostic = NetworkDiagnostic.runDiagnostic(host, tradingPort)
    KeywordUtil.markFailed("Trading Login gagal. Diagnosa: ${diagnostic.diagnosis}")
    // PENTING: return WAJIB di sini - tanpa ini, eksekusi tetap lanjut ke
    // placeOrder() meski Trading Login gagal total, menyebabkan error
    // "serverSessionId wajib diisi" karena session belum pernah terbentuk.
    return
}

// ============================================================
// STEP 4: KIRIM ORDER BUY (SOCKET)
// ============================================================
KeywordUtil.logInfo('='.multiply(60))
KeywordUtil.logInfo("📤 MENGIRIM ORDER: ${orderSide == '1' ? 'BUY' : 'SELL'} ${stockCode}${boardCode} | Lot: ${orderLot} | Price: ${orderPrice}")
KeywordUtil.logInfo('='.multiply(60))

Map orderResult = tradingClient.placeOrder(
        stockCode,
        boardCode,
        orderSide,
        orderLot,
        orderPrice,
        userId
)

verify(orderResult.timeout != true, 'Order tidak timeout (dapat response dalam batas waktu)')

if (orderResult.timeout == true) {
    KeywordUtil.logInfo('⚠️ PERHATIAN: Status order TIDAK PASTI karena timeout. ' +
            'Cek manual via Basic Order List sebelum mengambil kesimpulan.')
} else {
    verify(orderResult.success == true, "Order tidak ditolak server (tag58: ${orderResult.tag58 ?: '-'})")

    if (!orderResult.success) {
        KeywordUtil.logInfo("❌ Order DITOLAK. Alasan: ${orderResult.tag58}")
    } else {
        KeywordUtil.logInfo("✅ Order diterima server (belum tentu FILLED, cuma diterima/tidak ditolak).")
    }
}

// ============================================================
// STEP 5: VERIFIKASI VIA BASIC ORDER LIST
// ============================================================
KeywordUtil.logInfo('='.multiply(60))
KeywordUtil.logInfo('📋 CEK BASIC ORDER LIST untuk konfirmasi tambahan')
KeywordUtil.logInfo('='.multiply(60))

Mobile.delay(2)

List<Map> orderList = tradingClient.getBasicOrderList(userId)

if (orderList.isEmpty()) {
    KeywordUtil.logInfo('Tidak ada order ditemukan di Basic Order List (atau masih tertunda/delay server).')
} else {
    KeywordUtil.logInfo("Ditemukan ${orderList.size()} order di daftar:")
    orderList.each { row ->
        KeywordUtil.logInfo("Order Row: ${row}")
    }
}

// ============================================================
// STEP 6: RINGKASAN
// ============================================================
KeywordUtil.logInfo('='.multiply(60))
KeywordUtil.logInfo("📊 RINGKASAN VERIFIKASI: ${passedChecks}/${totalChecks} PASSED")
KeywordUtil.logInfo('='.multiply(60))

// ============================================================
// STEP 7: TUTUP SOCKET
// ============================================================
tradingClient.closeSocket()