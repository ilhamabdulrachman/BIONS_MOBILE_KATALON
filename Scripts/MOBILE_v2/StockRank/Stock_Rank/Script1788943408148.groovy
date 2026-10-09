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

int TOTAL_SAHAM_DICEK = 100
int TOP_N = 10
int liveMonitorSeconds = 15
int BATCH_SIZE = 50   

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
    KeywordUtil.logInfo("Aplikasi dengan ID '${applicationID}' berhasil diluncurkan.")
} catch (Exception e) {
    KeywordUtil.markFailed('Gagal meluncurkan aplikasi. Pastikan aplikasi sudah terinstal di perangkat. Error: ' + e.getMessage())
    return null
}

// ============================================================
// STEP 3: LOGIN VIA UI MOBILE
// ============================================================
Mobile.setText(findTestObject('Login_firebase/User_id'), userId, 0)
Mobile.setText(findTestObject('Login_firebase/Pw'), password, 0)
Mobile.setText(findTestObject('Login_firebase/Pin'), pin, 0)
Mobile.takeScreenshot("${screenshotBasePath}/Login0.PNG")

Instant loginStart = Instant.now()
Mobile.tap(findTestObject('Login_V2/button_login'), 0)
Mobile.takeScreenshot("${screenshotBasePath}/Login1.PNG")

boolean loginSuccess = Mobile.verifyElementExist(findTestObject('Login_V2/button_notnow'), 10, FailureHandling.OPTIONAL)

if (!loginSuccess) {
    Mobile.takeScreenshot("${screenshotBasePath}/Login2.PNG")
    Map diagnostic = NetworkDiagnostic.runDiagnostic(host, feedPort)
    KeywordUtil.markFailed('❌ Login GAGAL - biometric prompt (button_notnow) tidak muncul dalam 10 detik setelah tap Login. ' +
            "Diagnosa: ${diagnostic.diagnosis} " +
            'Cek screenshot \'Login2.PNG\' untuk kondisi layar saat verifikasi gagal.')
    return null
}

Instant loginEnd = Instant.now()
double loginSeconds = Duration.between(loginStart, loginEnd).toMillis() / 1000.0
KeywordUtil.logInfo("⏱️ Waktu login sampai dashboard: ${loginSeconds} detik")
KeywordUtil.logInfo('✅ Login berhasil terverifikasi - biometric prompt terdeteksi.')

NetworkDiagnostic.logDeviceNetworkInfo(deviceId)

def now = ZonedDateTime.now(ZoneId.of('Asia/Jakarta'))
def fmt = DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss')
KeywordUtil.logInfo('Login successful at ' + now.format(fmt))

// ============================================================
// STEP 4: HANDLE BIOMETRIC PROMPT
// ============================================================
Mobile.takeScreenshot("${screenshotBasePath}/Login4.PNG")
Mobile.tap(findTestObject('Login_V2/button_notnow'), 0)

// ============================================================
// STEP 5: NAVIGASI KE HALAMAN STOCK RANK (UI)
// ============================================================
Mobile.tap(findTestObject('Explore/See_All_explore'), 0)
Mobile.tap(findTestObject('Explore/Stock_Rank'), 0)
Mobile.takeScreenshot("${screenshotBasePath}/StockRank_UI.PNG")

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
    KeywordUtil.logInfo("Feed Login: ${feedResult.success}")
} catch (Exception e) {
    KeywordUtil.logInfo("❌ Feed Login gagal: ${e.message}")
    Map diagnostic = NetworkDiagnostic.runDiagnostic(host, feedPort)
    KeywordUtil.markFailed("Feed Login gagal. Diagnosa: ${diagnostic.diagnosis}")
    return null
}

// ============================================================
// STEP 7: AMBIL DAFTAR KODE SAHAM (dari Stock Master) - SEMUA SAHAM,
// BUKAN CUMA 100 PERTAMA. Stock Master TIDAK diurutkan berdasarkan
// aktivitas/frekuensi, jadi kita perlu cek SEMUA saham supaya ranking
// Top Frequency akurat (bukan cuma dari sampel kecil yang bisa saja
// tidak memuat saham paling aktif sama sekali).
// Filter HANYA kode saham standar (4 huruf) - buang warrant (-W),
// right (-R), dan kode obligasi/produk terstruktur.
// ============================================================
List<List> stockMasterRaw = feedClient.getStockMaster()
int totalDiServer = stockMasterRaw.size()
KeywordUtil.logInfo("📋 Total instrumen di server: ${totalDiServer}")

List<String> stockCodes = []
stockMasterRaw.each { row ->
    Map stock = BionsSocketClient.parseStockMasterRow(row)
    if (stock != null && stock.code != null) {
        String code = stock.code.toString()
        if (code ==~ /^[A-Z]{4}$/) {
            stockCodes << "${code}RG".toString()
        }
    }
}
KeywordUtil.logInfo("📋 Total kode saham standar (setelah filter): ${stockCodes.size()}")

// ============================================================
// STEP 8: QUERY STOCK SUMMARY PER BATCH (supaya tidak terlalu besar
// dalam 1 request) - hasil semua batch digabung sebelum diranking.
// ============================================================
List<Map> summaries = []
List<List> codeBatches = stockCodes.collate(BATCH_SIZE)
KeywordUtil.logInfo("📋 Query akan dilakukan dalam ${codeBatches.size()} batch (@ ${BATCH_SIZE} saham)")

codeBatches.eachWithIndex { batch, idx ->
    KeywordUtil.logInfo("   Batch ${idx + 1}/${codeBatches.size()} (${batch.size()} saham)...")
    List<List> summaryRawBatch = feedClient.getStockSummarySnapshot(batch)
    summaryRawBatch.each { row ->
        Map summary = BionsSocketClient.parseStockSummaryRow(row)
        if (summary != null && summary.tradeFrequency != null) {
            summaries << summary
        }
    }
}

int totalValid = summaries.size()
KeywordUtil.logInfo("📋 Total data valid untuk diranking: ${totalValid} saham")

// ============================================================
// STEP 9: RANKING - TOP 10 BERDASARKAN FREQUENCY
// ============================================================
List<Map> topFrequency = summaries.findAll { s ->
    s.tradeFrequency != null
}.sort { a, b ->
    (b.tradeFrequency as BigDecimal) <=> (a.tradeFrequency as BigDecimal)
}.take(TOP_N)

int totalTopFrequency = topFrequency.size()
KeywordUtil.logInfo("📋 Saham masuk Top ${TOP_N} Frequency: ${totalTopFrequency}")

// ============================================================
// STEP 10: CEK REALTIME - SUBSCRIBE LIVE QUOTE HANYA UNTUK TOP 10 INI
// ASUMSI: semua pakai board 'RG' (Reguler).
// ============================================================
KeywordUtil.logInfo('='.multiply(60))
KeywordUtil.logInfo("🔴 MENGECEK REALTIME untuk ${totalTopFrequency} saham Top Frequency (${liveMonitorSeconds} detik)...")
KeywordUtil.logInfo('='.multiply(60))

List<String> symbolsWithBoard = topFrequency.collect { s -> "${s.stockCode}RG".toString() }

Map<String, String> subscriptions = feedClient.subscribeMultipleStockQuotes(symbolsWithBoard)
feedClient.listen(liveMonitorSeconds)
Map<String, List> allUpdates = feedClient.parseAllLiveQuoteUpdates()
List<String> noMovementSymbols = BionsSocketClient.getStocksWithNoMovement(symbolsWithBoard, allUpdates)
feedClient.unsubscribeMultipleStockQuotes(subscriptions)

int totalRealtimeOk = totalTopFrequency - noMovementSymbols.size()
KeywordUtil.logInfo("✅ Realtime terkonfirmasi : ${totalRealtimeOk} saham")
KeywordUtil.logInfo("⚠️ Tidak ada update      : ${noMovementSymbols.size()} saham")
KeywordUtil.logInfo('='.multiply(60))

// ============================================================
// STEP 11: TAMPILKAN LIST TOP 10 FREQUENCY + STATUS REALTIME
// ============================================================
KeywordUtil.logInfo('='.multiply(60))
KeywordUtil.logInfo("🔥 TOP ${TOP_N} FREQUENCY (Frekuensi Transaksi Tertinggi) + STATUS REALTIME")
KeywordUtil.logInfo('='.multiply(60))

int rankNum = 1
topFrequency.each { s ->
    String code = s.stockCode ?: '-'
    String frequency = s.tradeFrequency ?: '-'
    String volume = s.tradeVolume ?: '-'
    String close = s.close ?: '-'
    String symbolWithBoard = "${code}RG".toString()

    boolean isNotRealtime = noMovementSymbols.contains(symbolWithBoard)
    String realtimeTag = isNotRealtime ? '⚠️ TIDAK REALTIME' : '✅ REALTIME'

    KeywordUtil.logInfo("${rankNum}. ${code} - Rp${close} | Frequency: ${frequency} | Volume: ${volume} [${realtimeTag}]")
    rankNum++
}

KeywordUtil.logInfo('='.multiply(60))
KeywordUtil.logInfo("✅ Ranking selesai. ${totalRealtimeOk}/${totalTopFrequency} saham terkonfirmasi realtime.")
KeywordUtil.logInfo('='.multiply(60))

// ============================================================
// STEP 12: TUTUP SOCKET
// ============================================================
feedClient.closeSocket()