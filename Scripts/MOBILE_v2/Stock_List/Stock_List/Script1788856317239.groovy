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
import com.utilities.TradingHours as TradingHours
import com.utilities.BionsSocketClient as BionsSocketClient
import com.utilities.NetworkDiagnostic as NetworkDiagnostic

// ============================================================
// KONFIGURASI
// ============================================================
String applicationID = 'id.bions.bnis.android.new_bions_revamp'

String screenshotBasePath = '/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login'

String userId = GlobalVariable.G_userId

String password = GlobalVariable.G_password

String pin = GlobalVariable.G_pin

String host = GlobalVariable.G_tradinghost

int feedPort = GlobalVariable.G_feedport.toInteger()

int tradingPort = GlobalVariable.G_tradingport.toInteger()

String clientIp = GlobalVariable.G_clientip

String deviceId = GlobalVariable.G_deviceId

int TOTAL_SAHAM_DIAMBIL = 100

int liveMonitorSeconds = 15

// ============================================================
// STEP 1: CEK JAM BURSA
// ============================================================
//if (!(TradingHours.isMarketOpen())) {
//    KeywordUtil.markFailed('Tes gagal. Bursa sedang tutup.')
//    return null
//}
// ============================================================
// STEP 2: LAUNCH APPLICATION
// ============================================================
try {
    Mobile.startExistingApplication(applicationID, FailureHandling.STOP_ON_FAILURE)

    KeywordUtil.logInfo("Aplikasi dengan ID '$applicationID' berhasil diluncurkan.")
}
catch (Exception e) {
    KeywordUtil.markFailed('Gagal meluncurkan aplikasi. Pastikan aplikasi sudah terinstal di perangkat. Error: ' + e.getMessage())

    return null
} 

// ============================================================
// STEP 3: LOGIN VIA UI MOBILE
// ============================================================
Mobile.setText(findTestObject('Login_firebase/User_id'), userId, 0)

Mobile.setText(findTestObject('Login_firebase/Pw'), password, 0)

Mobile.setText(findTestObject('Login_firebase/Pin'), pin, 0)

Mobile.takeScreenshot("$screenshotBasePath/Login0.PNG")

Instant loginStart = Instant.now()

Mobile.tap(findTestObject('Login_V2/button_login'), 0)

Mobile.takeScreenshot("$screenshotBasePath/Login1.PNG")

boolean loginSuccess = Mobile.verifyElementExist(findTestObject('Login_V2/button_notnow'), 10, FailureHandling.OPTIONAL)

if (!(loginSuccess)) {
    Mobile.takeScreenshot("$screenshotBasePath/Login2.PNG")

    Map diagnostic = NetworkDiagnostic.runDiagnostic(host, feedPort)

    KeywordUtil.markFailed(('❌ Login GAGAL - biometric prompt (button_notnow) tidak muncul dalam 10 detik setelah tap Login. ' + 
        "Diagnosa: $diagnostic.diagnosis ") + 'Cek screenshot \'Login2.PNG\' untuk kondisi layar saat verifikasi gagal.')

    return null
}

Instant loginEnd = Instant.now()

double loginSeconds = Duration.between(loginStart, loginEnd).toMillis() / 1000.0

KeywordUtil.logInfo("⏱️ Waktu login sampai dashboard: $loginSeconds detik")

KeywordUtil.logInfo('✅ Login berhasil terverifikasi - biometric prompt terdeteksi.')

NetworkDiagnostic.logDeviceNetworkInfo(deviceId)

def now = ZonedDateTime.now(ZoneId.of('Asia/Jakarta'))

def fmt = DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss')

KeywordUtil.logInfo('Login successful at ' + now.format(fmt))

// ============================================================
// STEP 4: HANDLE BIOMETRIC PROMPT
// ============================================================
Mobile.takeScreenshot("$screenshotBasePath/Login4.PNG")

Mobile.tap(findTestObject('Login_V2/button_notnow'), 0)

// ============================================================
// STEP 5: NAVIGASI KE HALAMAN STOCK LIST (UI)
// ============================================================
Mobile.tap(findTestObject('Explore/See_All_explore'), 0)

Mobile.tap(findTestObject('Explore/A_stock_list'), 0)

Mobile.swipe(500, 1500, 500, 500)

Mobile.takeScreenshot("$screenshotBasePath/Trade1_V2.PNG")

Mobile.swipe(500, 1500, 500, 500)

Mobile.takeScreenshot("$screenshotBasePath/Trade2_V2.PNG")

Mobile.swipe(500, 1500, 500, 500)

Mobile.takeScreenshot("$screenshotBasePath/Trade3_V2.PNG")

Mobile.swipe(500, 1500, 500, 500)

Mobile.takeScreenshot("$screenshotBasePath/Trade4_V2.PNG")

Mobile.closeApplication()

// CATATAN: Semua interaksi UI SUDAH SELESAI di titik ini. Socket login
// di bawah kemungkinan membuat sesi UI ter-logout (kebijakan single-session
// server) - ini sudah diperhitungkan dalam desain test case ini.
// ============================================================
// STEP 6: SOCKET FEED LOGIN
// ============================================================
BionsSocketClient feedClient = new BionsSocketClient()

try {
    Map feedResult = feedClient.loginFeed(host, feedPort, userId, password, clientIp)

    KeywordUtil.logInfo("Feed Login: $feedResult.success")
}
catch (Exception e) {
    KeywordUtil.logInfo("❌ Feed Login gagal: $e.message")

    Map diagnostic = NetworkDiagnostic.runDiagnostic(host, feedPort)

    KeywordUtil.markFailed("Feed Login gagal. Diagnosa: $diagnostic.diagnosis")

    return null
} 

// ============================================================
// STEP 7: AMBIL DAFTAR SAHAM (STOCK MASTER) VIA SOCKET - AMBIL 100 SAJA
// ============================================================
List<List> stockMasterRaw = feedClient.getStockMaster()

// Hitung dulu semua nilai yang butuh .size()/.toString() SEBELUM masuk
// ke template HTML - supaya template di bawah cuma berisi nama variabel
// polos, tidak ada pemanggilan method yang bisa rusak saat copy-paste.
int totalDiServer = stockMasterRaw.size()

KeywordUtil.logInfo('='.multiply(60))

KeywordUtil.logInfo("📋 TOTAL SAHAM TERSEDIA DI SERVER: $totalDiServer")

KeywordUtil.logInfo("📋 DIAMBIL UNTUK LAPORAN: $TOTAL_SAHAM_DIAMBIL saham pertama")

KeywordUtil.logInfo('='.multiply(60))

List<Map> parsedStocks = []

stockMasterRaw.take(TOTAL_SAHAM_DIAMBIL).each({ def row ->
        Map stock = BionsSocketClient.parseStockMasterRow(row)

        if (stock != null) {
            parsedStocks << stock
        }
    })

int totalDiLaporan = parsedStocks.size()

KeywordUtil.logInfo("Berhasil parsing $totalDiLaporan saham untuk laporan.")

// ============================================================
// STEP 8: CEK REALTIME - SUBSCRIBE LIVE QUOTE UNTUK SEMUA SAHAM
// DI LAPORAN, PANTAU BEBERAPA DETIK, LIHAT MANA YANG TIDAK DAPAT UPDATE.
// ASUMSI: semua pakai board 'RG' (Reguler) - saham dengan board lain
// mungkin salah terdeteksi "tidak realtime" walau sebenarnya aktif.
// ============================================================
KeywordUtil.logInfo('='.multiply(60))

KeywordUtil.logInfo("🔴 MENGECEK REALTIME untuk $totalDiLaporan saham ($liveMonitorSeconds detik)...")

KeywordUtil.logInfo('='.multiply(60))

List<String> symbolsWithBoard = parsedStocks.collect({ def stock ->
        "${stock.code}RG".toString()
    })

Map<String, String> subscriptions = feedClient.subscribeMultipleStockQuotes(symbolsWithBoard)

feedClient.listen(liveMonitorSeconds)

Map<String, List> allUpdates = feedClient.parseAllLiveQuoteUpdates()

List<String> noMovementSymbols = BionsSocketClient.getStocksWithNoMovement(symbolsWithBoard, allUpdates)

feedClient.unsubscribeMultipleStockQuotes(subscriptions)

int totalRealtimeOk = totalDiLaporan - noMovementSymbols.size()

KeywordUtil.logInfo("✅ Realtime terkonfirmasi : $totalRealtimeOk saham")

KeywordUtil.logInfo("⚠️ Tidak ada update      : ${noMovementSymbols.size()} saham")

KeywordUtil.logInfo('='.multiply(60))

// ============================================================
// STEP 9: TAMPILKAN SEBAGAI NUMBERED LIST LANGSUNG DI LOG
// (tidak perlu buka file terpisah - cukup lihat di Console Katalon)
// Setiap saham ditandai ✅ REALTIME atau ⚠️ TIDAK REALTIME.
// ============================================================
KeywordUtil.logInfo('='.multiply(60))

KeywordUtil.logInfo("📋 DAFTAR $totalDiLaporan SAHAM")

KeywordUtil.logInfo('='.multiply(60))

int listNum = 1

parsedStocks.each({ def stock ->
        String code = stock.code ?: '-'

        String stockName = stock.name ?: '-'

        String status = stock.status ?: '-'

        String lotSize = stock.lotSize ?: '-'

        String basePrice = stock.basePrice ?: '-'

       String symbolWithBoard = "${code}RG".toString()

        boolean isNotRealtime = noMovementSymbols.contains(symbolWithBoard)

        String realtimeTag = isNotRealtime ? '⚠️ TIDAK REALTIME' : '✅ REALTIME'

        KeywordUtil.logInfo("$listNum. $code - $stockName [$realtimeTag]")

        KeywordUtil.logInfo("   Status: $status | Lot: $lotSize | Harga Dasar: $basePrice")

        listNum++
    })

KeywordUtil.logInfo('='.multiply(60))

KeywordUtil.logInfo("✅ Total $totalDiLaporan saham berhasil ditampilkan.")

KeywordUtil.logInfo('='.multiply(60))

// ============================================================
// STEP 10: DAFTAR TERPISAH - SAHAM YANG TIDAK REALTIME
// ============================================================
KeywordUtil.logInfo('='.multiply(60))

KeywordUtil.logInfo("⚠️ DAFTAR SAHAM TIDAK REALTIME (${noMovementSymbols.size()} saham)")

KeywordUtil.logInfo('   Kemungkinan: harga memang stabil/market sepi, ATAU ada masalah subscription.')

KeywordUtil.logInfo('='.multiply(60))

if (noMovementSymbols.isEmpty()) {
    KeywordUtil.logInfo('Tidak ada - SEMUA saham terkonfirmasi realtime! 🎉')
} else {
    int notRealtimeNum = 1

    noMovementSymbols.each({ def symbolWithBoard ->
            KeywordUtil.logInfo("$notRealtimeNum. $symbolWithBoard")

            notRealtimeNum++
        })
}

KeywordUtil.logInfo('='.multiply(60))

// ============================================================
// STEP 11: TUTUP SOCKET
// ============================================================
feedClient.closeSocket()

