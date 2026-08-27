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
import com.utilities.NetworkDiagnostic

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

List<String> symbols = ['BBCARG', 'BBNIRG', 'TLKMRG']
int liveMonitorSeconds = 10

// ============================================================
// STEP 1: CEK JAM BURSA
// ============================================================

//if (!(TradingHours.isMarketOpen())) {
//    KeywordUtil.markFailed('Tes gagal. Bursa sedang tutup.')
//}

// ============================================================
// STEP 2: LAUNCH APPLICATION
// ============================================================
try {
    Mobile.startExistingApplication(applicationID, FailureHandling.STOP_ON_FAILURE)
    KeywordUtil.logInfo("Aplikasi dengan ID '${applicationID}' berhasil diluncurkan.")
} catch (Exception e) {
    KeywordUtil.markFailed('Gagal meluncurkan aplikasi. Pastikan aplikasi sudah terinstal di perangkat. Error: ' + e.getMessage())
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

// ===== VERIFIKASI NYATA: cek apakah button_notnow (biometric prompt) muncul =====

boolean loginSuccess = Mobile.verifyElementExist(
        findTestObject('Login_V2/button_notnow'),
        10,
        FailureHandling.OPTIONAL
)

if (!loginSuccess) {
    Mobile.takeScreenshot("${screenshotBasePath}/Login_FAILED.PNG")

    // Jalankan diagnosa jaringan untuk cari tahu penyebab kegagalan login
    Map diagnostic = NetworkDiagnostic.runDiagnostic(host, feedPort)

    KeywordUtil.markFailed('❌ Login GAGAL - biometric prompt (button_notnow) tidak muncul dalam 10 detik setelah tap Login. ' +
            "Diagnosa: ${diagnostic.diagnosis} " +
            "Cek screenshot 'Login_FAILED.PNG' untuk kondisi layar saat verifikasi gagal.")
}

Instant loginEnd = Instant.now()
double loginSeconds = Duration.between(loginStart, loginEnd).toMillis() / 1000.0
KeywordUtil.logInfo("⏱️ Waktu login sampai dashboard: ${loginSeconds} detik")
KeywordUtil.logInfo('✅ Login berhasil terverifikasi - biometric prompt terdeteksi.')

// ===== INFO JARINGAN HP ANDROID =====
NetworkDiagnostic.logDeviceNetworkInfo(deviceId)

def now = ZonedDateTime.now(ZoneId.of('Asia/Jakarta'))
def fmt = DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss')
KeywordUtil.logInfo('Login successful at ' + now.format(fmt))

// ============================================================
// STEP 4: HANDLE BIOMETRIC PROMPT
// ============================================================
Mobile.takeScreenshot("${screenshotBasePath}/Login_Biometric.PNG")
Mobile.tap(findTestObject('Login_V2/button_notnow'), 0)
Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Trade_V2.PNG')

Mobile.swipe(500, 1500, 500, 500)
Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Trade1_V2.PNG')

Mobile.swipe(500, 1500, 500, 500)
Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Trade2_V2.PNG')

Mobile.swipe(500, 1500, 500, 500)
Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Trade3_V2.PNG')

Mobile.swipe(500, 1500, 500, 500)
Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Trade4_V2.PNG')

Mobile.swipe(500, 1500, 500, 500)
Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Trade5_V2.PNG')

Mobile.swipe(500, 1500, 500, 500)
Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Trade6_V2.PNG')


// ============================================================
// STEP 5: SOCKET FEED LOGIN
// ============================================================
BionsSocketClient feedClient = new BionsSocketClient()

Instant feedLoginStart = Instant.now()
double feedLoginSeconds = 0.0   // Dideklarasikan DI LUAR try, supaya bisa diakses di STEP 11

try {
    Map feedResult = feedClient.loginFeed(host, feedPort, userId, password, clientIp)
    Instant feedLoginEnd = Instant.now()

    feedLoginSeconds = Duration.between(feedLoginStart, feedLoginEnd).toMillis() / 1000.0
    KeywordUtil.logInfo("⏱️ Waktu Feed Login: ${feedLoginSeconds} detik")
    KeywordUtil.logInfo("✅ Feed Login berhasil! Gateway: ${feedResult.gatewayId}, AutoRenew: ${feedResult.autoRenew}")

} catch (Exception e) {
    KeywordUtil.logInfo("❌ Login Feed gagal: ${e.message}")
    Map diagnostic = NetworkDiagnostic.runDiagnostic(host, feedPort)
    KeywordUtil.markFailed("Login gagal. Diagnosa: ${diagnostic.diagnosis}")
}

// ============================================================
// STEP 6: SOCKET TRADING LOGIN
// ============================================================
BionsSocketClient tradingClient = new BionsSocketClient()

Instant tradingLoginStart = Instant.now()
double tradingLoginSeconds = 0.0

try {
    Map tradingResult = tradingClient.loginTrading(host, tradingPort, userId, password, pin, clientIp)
    Instant tradingLoginEnd = Instant.now()

    tradingLoginSeconds = Duration.between(tradingLoginStart, tradingLoginEnd).toMillis() / 1000.0
    KeywordUtil.logInfo("⏱️ Waktu Trading Login: ${tradingLoginSeconds} detik")
    KeywordUtil.logInfo("✅ Trading Login berhasil! Tag58 (Gateway): ${tradingResult.tag58Value}")

} catch (Exception e) {
    KeywordUtil.logInfo("❌ Login Trading gagal: ${e.message}")
    Map diagnostic = NetworkDiagnostic.runDiagnostic(host, tradingPort)
    KeywordUtil.markFailed("Login Trading gagal. Diagnosa: ${diagnostic.diagnosis}")
}

// ============================================================
// STEP 7: STOCK QUOTE SNAPSHOT
// ============================================================
KeywordUtil.logInfo('=== STOCK QUOTE SNAPSHOT ===')

symbols.each { symbol ->
    List quoteRow = feedClient.getStockQuoteSnapshot(symbol)

    if (quoteRow != null) {
        Map quote = BionsSocketClient.parseStockQuoteRow(quoteRow)
        KeywordUtil.logInfo("${symbol}: Rp${BionsSocketClient.formatPrice(quote.displayLast)} | Change: ${quote.change} (${quote.changePct}%) | Bid: ${quote.bestBid} / Offer: ${quote.bestOffer}")
    } else {
        KeywordUtil.logInfo("${symbol}: Tidak ada data quote")
    }
}

// ============================================================
// STEP 8: LIVE UPDATE MULTI-SAHAM
// ============================================================
KeywordUtil.logInfo("=== LIVE UPDATE (${liveMonitorSeconds} detik) ===")

Map<String, String> subscriptions = feedClient.subscribeMultipleStockQuotes(symbols)
feedClient.listen(liveMonitorSeconds)
Map<String, List> allUpdates = feedClient.parseAllLiveQuoteUpdates()

allUpdates.each { stockCode, quotes ->
    quotes.each { quote ->
        KeywordUtil.logInfo("${stockCode}: Rp${BionsSocketClient.formatPrice(quote.displayLast)} (Change: ${quote.change} / ${quote.changePct}%)")
    }
}

List<String> noMovement = BionsSocketClient.getStocksWithNoMovement(symbols, allUpdates)

if (!noMovement.isEmpty()) {
    KeywordUtil.logInfo("ℹ️ Saham tanpa pergerakan: ${noMovement}")
}

feedClient.unsubscribeMultipleStockQuotes(subscriptions)

// ============================================================
// STEP 9: MARKET INFO (IHSG)
// ============================================================
KeywordUtil.logInfo('=== MARKET INFO ===')

Map marketInfo = feedClient.getMarketInfoSnapshot()

if (marketInfo != null) {
    KeywordUtil.logInfo("IHSG: ${BionsSocketClient.formatPrice(marketInfo.last)} (${marketInfo.changePct}%)")
} else {
    KeywordUtil.logInfo('Tidak ada data Market Info')
}

// ============================================================
// STEP 10: PORTFOLIO STOCK (pakai tradingClient, SEBELUM ditutup)
// ============================================================
KeywordUtil.logInfo('=== PORTFOLIO STOCK ===')

List<List> portfolioRows = tradingClient.getPortfolioStockSnapshot(userId)

if (portfolioRows.isEmpty()) {
    KeywordUtil.logInfo("Tidak ada data Portfolio Stock untuk user ${userId}.")
} else {
    portfolioRows.each { row ->
        KeywordUtil.logInfo("Portfolio Row: ${row}")
    }
}

// ============================================================
// STEP 11: RINGKASAN WAKTU
// ============================================================
KeywordUtil.logInfo('='.multiply(50))
KeywordUtil.logInfo('📊 RINGKASAN WAKTU')
KeywordUtil.logInfo('='.multiply(50))
KeywordUtil.logInfo("UI Login          : ${loginSeconds} detik")
KeywordUtil.logInfo("Feed Login        : ${feedLoginSeconds} detik")
KeywordUtil.logInfo("Trading Login     : ${tradingLoginSeconds} detik")
KeywordUtil.logInfo('='.multiply(50))

// ============================================================
// STEP 12: TUTUP SEMUA SOCKET & APLIKASI (PALING AKHIR)
// ============================================================
feedClient.closeSocket()
tradingClient.closeSocket()
Mobile.closeApplication()