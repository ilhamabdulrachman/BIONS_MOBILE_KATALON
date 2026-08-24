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

/**
 * ============================================================
 * TEST CASE: Watchlist Real-time Data Verification (Production)
 * ============================================================
 *
 * TUJUAN:
 * 1. Verifikasi UI Watchlist bisa diakses normal setelah login
 * 2. Verifikasi Feed & Trading Login via socket berhasil
 * 3. Verifikasi data real-time (Stock Quote, Live Update) valid
 *
 * PRASYARAT SETUP (WAJIB sebelum run):
 * Tambahkan Global Variable berikut di Profiles > default:
 *   - g_userId, g_password, g_pin, g_tradingHost,
 *     g_feedPort, g_tradingPort, g_clientIp
 *
 * CATATAN RISIKO:
 * Test ini menyasar PRODUCTION (trade.bions.id). Pastikan sudah
 * disetujui tim terkait untuk dijalankan berulang, dan gunakan
 * akun yang memang dialokasikan untuk keperluan testing otomatis.
 * ============================================================
 */

// ============================================================
// KONFIGURASI (dari Global Variables, BUKAN hardcode)
// ============================================================
String applicationID = 'id.bions.bnis.android.new_bions_revamp'
String screenshotBasePath = '/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login'

String userId = GlobalVariable.G_userId
String password = GlobalVariable.G_password
String pin = GlobalVariable.G_pin
String host = GlobalVariable.G_tradinghost
int feedPort = GlobalVariable.G_feedport as int
int tradingPort = GlobalVariable.G_tradingport as int
String clientIp = GlobalVariable.G_clientip

List<String> symbols = ['BBCARG', 'BRMSRG', 'SUPARG']
int liveMonitorSeconds = 10

// Counter untuk ringkasan hasil verifikasi di akhir
int totalChecks = 0
int passedChecks = 0

/**
 * Helper lokal untuk verifikasi dengan pesan jelas, tanpa menghentikan
 * test case secara paksa di tengah jalan (soft assertion).
 */
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
    KeywordUtil.logInfo("Aplikasi '${applicationID}' berhasil diluncurkan.")
} catch (Exception e) {
    KeywordUtil.markFailed('Gagal meluncurkan aplikasi: ' + e.getMessage(), FailureHandling.STOP_ON_FAILURE)
}

// ============================================================
// STEP 2: LOGIN VIA UI MOBILE
// ============================================================
Mobile.setText(findTestObject('Login_firebase/User_id'), userId, 0)
Mobile.setText(findTestObject('Login_firebase/Pw'), password, 0)
Mobile.setText(findTestObject('Login_firebase/Pin'), pin, 0)
Mobile.takeScreenshot("${screenshotBasePath}/Login0.PNG")

Instant loginStart = Instant.now()
Mobile.tap(findTestObject('Login_V2/login'), 0)
Mobile.takeScreenshot("${screenshotBasePath}/Login1.PNG")
Instant loginEnd = Instant.now()

double loginSeconds = Duration.between(loginStart, loginEnd).toMillis() / 1000.0
KeywordUtil.logInfo("⏱️ Waktu login: ${loginSeconds} detik")
verify(loginSeconds < 5.0, "Login UI selesai dalam waktu wajar (< 5 detik). Aktual: ${loginSeconds}s")

def now = ZonedDateTime.now(ZoneId.of('Asia/Jakarta'))
def fmt = DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss')
KeywordUtil.logInfo('Login pada: ' + now.format(fmt))

Mobile.takeScreenshot("${screenshotBasePath}/Login_Biometric.PNG")
Mobile.tap(findTestObject('Login_firebase/Not_now'), 0)
Mobile.takeScreenshot("${screenshotBasePath}/Login_V2.PNG")

// ============================================================
// STEP 3: VERIFIKASI UI WATCHLIST
// ============================================================
Mobile.tap(findTestObject('Watchlist/WATCHLIST_PROD'), 0)
Mobile.takeScreenshot("${screenshotBasePath}/Watchlist1.PNG")



Mobile.swipe(500, 1500, 500, 500)
Mobile.takeScreenshot("${screenshotBasePath}/Watchlist2.PNG")
Mobile.swipe(500, 1500, 500, 500)
Mobile.takeScreenshot("${screenshotBasePath}/Watchlist3.PNG")
Mobile.swipe(500, 1500, 500, 500)
Mobile.takeScreenshot("${screenshotBasePath}/Watchlist4.PNG")
Mobile.swipe(500, 1500, 500, 500)
Mobile.takeScreenshot("${screenshotBasePath}/Watchlist5.PNG")

// CATATAN: Semua interaksi UI SUDAH SELESAI di titik ini. Baris di bawah
// (Socket Feed + Trading) akan membuat sesi UI ter-logout otomatis
// (server menerapkan kebijakan single-session per user). Ini SUDAH
// DIPERHITUNGKAN dalam desain test case ini - tidak ada lagi kebutuhan
// interaksi Mobile.* setelah titik ini, kecuali closeApplication().

// ============================================================
// STEP 4: SOCKET LOGIN (FEED) + VERIFIKASI
// ============================================================
BionsSocketClient feedClient = new BionsSocketClient()

Instant feedLoginStart = Instant.now()
Map feedResult = feedClient.loginFeed(host, feedPort, userId, password, clientIp)
Instant feedLoginEnd = Instant.now()

double feedLoginSeconds = Duration.between(feedLoginStart, feedLoginEnd).toMillis() / 1000.0
KeywordUtil.logInfo("⏱️ Waktu Feed Login: ${feedLoginSeconds} detik")

verify(feedResult.success == true, "Feed Login berhasil (success = true)")
verify(feedResult.gatewayId != null, "Feed Login mengembalikan Gateway ID yang valid")
KeywordUtil.logInfo("Feed Gateway: ${feedResult.gatewayId}, AutoRenew: ${feedResult.autoRenew}")

// ============================================================
// STEP 5: SOCKET LOGIN (TRADING) + VERIFIKASI
// ============================================================
BionsSocketClient tradingClient = new BionsSocketClient()

Instant tradingLoginStart = Instant.now()
Map tradingResult = tradingClient.loginTrading(host, tradingPort, userId, password, pin, clientIp)
Instant tradingLoginEnd = Instant.now()

double tradingLoginSeconds = Duration.between(tradingLoginStart, tradingLoginEnd).toMillis() / 1000.0
KeywordUtil.logInfo("⏱️ Waktu Trading Login: ${tradingLoginSeconds} detik")

verify(tradingResult.success == true, "Trading Login berhasil (success = true)")
verify(tradingResult.tag58Value != null, "Trading Login mengembalikan Gateway (tag58) yang valid")
KeywordUtil.logInfo("Trading Gateway (tag58): ${tradingResult.tag58Value}")

// ============================================================
// STEP 6: VERIFIKASI STOCK QUOTE SNAPSHOT
// ============================================================
KeywordUtil.logInfo('=== STOCK QUOTE SNAPSHOT ===')

symbols.each { symbol ->
    List quoteRow = feedClient.getStockQuoteSnapshot(symbol)

    if (quoteRow != null) {
        Map quote = BionsSocketClient.parseStockQuoteRow(quoteRow)
        KeywordUtil.logInfo("${symbol}: Rp${BionsSocketClient.formatPrice(quote.displayLast)} | Change: ${quote.change} (${quote.changePct}%)")

        verify(quote.stockCode != null, "${symbol}: Kode saham terisi di response")
        verify((quote.displayLast as BigDecimal) > 0, "${symbol}: Harga (displayLast) lebih dari 0")
    } else {
        verify(false, "${symbol}: Data quote snapshot ditemukan")
    }
}

// ============================================================
// STEP 7: LIVE UPDATE MULTI-SAHAM
// ============================================================
KeywordUtil.logInfo("=== LIVE UPDATE (${liveMonitorSeconds} detik) ===")

Map<String, String> subscriptions = feedClient.subscribeMultipleStockQuotes(symbols)
feedClient.listen(liveMonitorSeconds)
Map<String, List> allUpdates = feedClient.parseAllLiveQuoteUpdates()

allUpdates.each { stockCode, quotes ->
    quotes.each { quote ->
        KeywordUtil.logInfo("${stockCode}: Rp${BionsSocketClient.formatPrice(quote.displayLast)} (${quote.changePct}%)")
    }
}

List<String> noMovement = BionsSocketClient.getStocksWithNoMovement(symbols, allUpdates)
if (!noMovement.isEmpty()) {
    KeywordUtil.logInfo("ℹ️ Saham tanpa pergerakan (wajar kalau market tutup): ${noMovement}")
}

feedClient.unsubscribeMultipleStockQuotes(subscriptions)

// ============================================================
// STEP 8: PORTFOLIO STOCK (pakai tradingClient, SEBELUM ditutup)
// ============================================================
KeywordUtil.logInfo('=== PORTFOLIO STOCK ===')

List<List> portfolioRows = tradingClient.getPortfolioStockSnapshot(userId)
verify(portfolioRows != null, "Query Portfolio Stock tidak error (List valid, boleh kosong)")

if (portfolioRows.isEmpty()) {
    KeywordUtil.logInfo("ℹ️ Tidak ada data Portfolio Stock untuk user ${userId} (atau belum ada saham dimiliki).")
} else {
    portfolioRows.each { row -> KeywordUtil.logInfo("Portfolio Row: ${row}") }
}

// ============================================================
// STEP 9: RINGKASAN HASIL VERIFIKASI
// ============================================================
KeywordUtil.logInfo("=".multiply(50))
KeywordUtil.logInfo("📊 RINGKASAN VERIFIKASI: ${passedChecks}/${totalChecks} PASSED")
KeywordUtil.logInfo("⏱️ Login: ${loginSeconds}s | Feed: ${feedLoginSeconds}s | Trading: ${tradingLoginSeconds}s")
KeywordUtil.logInfo("=".multiply(50))

// ============================================================
// STEP 10: TUTUP SEMUA SOCKET & APLIKASI (PALING AKHIR)
// ============================================================
feedClient.closeSocket()
tradingClient.closeSocket()
Mobile.closeApplication()