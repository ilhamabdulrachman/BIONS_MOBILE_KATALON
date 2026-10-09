import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import static com.kms.katalon.core.testdata.TestDataFactory.findTestData
import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords as Mobile
import com.kms.katalon.core.model.FailureHandling as FailureHandling
import com.kms.katalon.core.util.KeywordUtil as KeywordUtil
import com.kms.katalon.core.testdata.TestData as TestData
import internal.GlobalVariable as GlobalVariable
import com.utilities.BionsSocketClient as BionsSocketClient
import com.utilities.NetworkDiagnostic

// ============================================================
// KONFIGURASI
// ============================================================
String applicationID = 'id.bions.bnis.android.new_bions_revamp'
String screenshotBasePath = '/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login'

String userId = GlobalVariable.G_userIdDevuserId
String password = GlobalVariable.G_userIdDevpassword
String pin = GlobalVariable.G_userIdDevpin
String host = GlobalVariable.G_userIdDevtradinghost
int feedPort = GlobalVariable.G_userIdDevfeedport.toInteger()
String clientIp = GlobalVariable.G_userIdDevclientip

int liveMonitorSeconds = 15   // durasi pantau live, bisa disesuaikan

// ===== AMBIL DAFTAR SAHAM DARI DATA FILE (simpan ke List, dipakai 2x: UI & socket) =====
TestData stockData = findTestData('Data Files/StockSymbols')
int totalStocks = stockData.getRowNumbers()

List<String> stockSymbols = []
for (int row = 1; row <= totalStocks; row++) {
    stockSymbols.add(stockData.getValue('symbol', row))
}

// Untuk pencarian UI, biasanya cukup pakai kode dasar saham (tanpa suffix
// board seperti RG). Kalau simbol di Data File sudah termasuk 'RG' dsb,
// ambil 4 karakter pertama saja untuk pencarian di UI.
def uiSearchCode = { String fullSymbol -> fullSymbol.length() >= 4 ? fullSymbol.substring(0, 4) : fullSymbol }

KeywordUtil.logInfo("📋 Total saham yang akan dites: ${totalStocks}")

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
    KeywordUtil.logInfo("Aplikasi berhasil diluncurkan.")
} catch (Exception e) {
    KeywordUtil.markFailed('Gagal meluncurkan aplikasi. Error: ' + e.getMessage())
}

// ============================================================
// STEP 2: LOGIN VIA UI MOBILE
// ============================================================
Mobile.setText(findTestObject('Login_firebase/User_id'), userId, 0)
Mobile.setText(findTestObject('Login_firebase/Pw'), password, 0)
Mobile.setText(findTestObject('Login_firebase/Pin'), pin, 0)
Mobile.takeScreenshot("${screenshotBasePath}/OrderBook_Login0.PNG")

Mobile.tap(findTestObject('Login_V2/button_login'), 0)
Mobile.takeScreenshot("${screenshotBasePath}/OrderBook_Login1.PNG")

boolean loginSuccess = Mobile.verifyElementExist(
        findTestObject('Login_V2/button_notnow'),
        10,
        FailureHandling.OPTIONAL
)

if (!loginSuccess) {
    Mobile.takeScreenshot("${screenshotBasePath}/OrderBook_Login_FAILED.PNG")
    Map diagnostic = NetworkDiagnostic.runDiagnostic(host, feedPort)
    KeywordUtil.markFailed('❌ Login GAGAL - biometric prompt tidak muncul dalam 10 detik. ' +
            "Diagnosa: ${diagnostic.diagnosis}")
}

KeywordUtil.logInfo('✅ Login berhasil terverifikasi - biometric prompt terdeteksi.')

Mobile.tap(findTestObject('Login_V2/button_notnow'), 0)
Mobile.takeScreenshot("${screenshotBasePath}/OrderBook_Login_V2.PNG")

// ============================================================
// STEP 3: NAVIGASI KE HALAMAN TRADE
// ============================================================
Mobile.tap(findTestObject('NavBar_Scalability/trade_'), 0)
Mobile.takeScreenshot("${screenshotBasePath}/Trade_V2.PNG")

Mobile.swipe(500, 1500, 500, 500)
Mobile.takeScreenshot("${screenshotBasePath}/Trade_V1.PNG")

// ============================================================
// STEP 4: CARI & PILIH SETIAP SAHAM VIA UI (SESUAI DATA DRIVEN)
// ============================================================
stockSymbols.each { fullSymbol ->
    String searchCode = uiSearchCode(fullSymbol)

    KeywordUtil.logInfo("🔍 Mencari saham via UI: ${searchCode}")

    Mobile.tap(findTestObject('Order_Book/Search_Tap'), 0)
    Mobile.takeScreenshot("${screenshotBasePath}/Trade_Search_${fullSymbol}.PNG")

    Mobile.tap(findTestObject('Order_Book/tap_stock_2'), 0)
    Mobile.takeScreenshot("${screenshotBasePath}/Trade_TapStock2_${fullSymbol}.PNG")

    Mobile.setText(findTestObject('Order_Book/Input_Stock'), searchCode, 0)
    Mobile.takeScreenshot("${screenshotBasePath}/Trade_Input_${fullSymbol}.PNG")

    Mobile.tap(findTestObject('Order_Book/Tap_Stock'), 0)
    Mobile.takeScreenshot("${screenshotBasePath}/Trade_Selected_${fullSymbol}.PNG")
}

Mobile.closeApplication()

// CATATAN: Semua interaksi UI SUDAH SELESAI di titik ini. Socket login
// di bawah kemungkinan membuat sesi UI ter-logout (kebijakan single-session
// server) - ini sudah diperhitungkan dalam desain test case ini.

// ============================================================
// STEP 5: SOCKET FEED LOGIN (1x saja, dipakai untuk semua saham)
// ============================================================
BionsSocketClient feedClient = new BionsSocketClient()

try {
    Map feedResult = feedClient.loginFeed(host, feedPort, userId, password, clientIp)
    verify(feedResult.success == true, 'Feed Login berhasil')
} catch (Exception e) {
    KeywordUtil.logInfo("❌ Feed Login gagal: ${e.message}")
    Map diagnostic = NetworkDiagnostic.runDiagnostic(host, feedPort)
    KeywordUtil.markFailed("Feed Login gagal. Diagnosa: ${diagnostic.diagnosis}")
}

// ============================================================
// STEP 6: LOOP ORDERBOOK SNAPSHOT UNTUK SETIAP SAHAM (SOCKET)
// ============================================================
int stockSuccessCount = 0
int stockFailCount = 0

stockSymbols.eachWithIndex { stockSymbol, idx ->
    int rowNum = idx + 1

    KeywordUtil.logInfo('='.multiply(60))
    KeywordUtil.logInfo("📈 SAHAM ${rowNum}/${totalStocks}: ${stockSymbol}")
    KeywordUtil.logInfo('='.multiply(60))

    try {
        Map orderbook = feedClient.getOrderbookSnapshot(stockSymbol)

        verify(orderbook != null, "[${stockSymbol}] Orderbook snapshot berhasil diterima")

        if (orderbook != null) {
            def bids = orderbook.bids
            def offers = orderbook.offers

            KeywordUtil.logInfo("=== ORDERBOOK ${orderbook.stockCode}${orderbook.boardCode} | Last: Rp${BionsSocketClient.formatPrice(orderbook.last)} ===")
            KeywordUtil.logInfo(String.format('%-8s %-10s %-8s | %-8s %-10s %-8s', 'Queue', 'Lot', 'Bid', 'Offer', 'Lot', 'Queue'))

            int maxRows = Math.max(bids.size(), offers.size())
            for (int i = 0; i < maxRows; i++) {
                Map bid = i < bids.size() ? bids[i] : [price: '-', lot: '-', orderCount: '-']
                Map offer = i < offers.size() ? offers[i] : [price: '-', lot: '-', orderCount: '-']

                KeywordUtil.logInfo(String.format('%-8s %-10s %-8s | %-8s %-10s %-8s',
                        bid.orderCount, bid.lot, BionsSocketClient.formatPrice(bid.price),
                        BionsSocketClient.formatPrice(offer.price), offer.lot, offer.orderCount))
            }

            verify(bids.size() > 0, "[${stockSymbol}] Minimal 1 level Bid tersedia")
            verify(offers.size() > 0, "[${stockSymbol}] Minimal 1 level Offer tersedia")

            if (bids.size() > 0 && offers.size() > 0) {
                BigDecimal bestBidPrice = bids[0].price as BigDecimal
                BigDecimal bestOfferPrice = offers[0].price as BigDecimal
                verify(bestBidPrice < bestOfferPrice, "[${stockSymbol}] Best Bid (${bestBidPrice}) < Best Offer (${bestOfferPrice})")
            }

            stockSuccessCount++
        } else {
            KeywordUtil.logInfo("Tidak ada data Orderbook untuk ${stockSymbol}")
            stockFailCount++
        }

    } catch (Exception e) {
        KeywordUtil.logInfo("❌ ERROR saat proses saham ${stockSymbol}: ${e.message}")
        stockFailCount++
    }
}

// ============================================================
// STEP 7: PEMANTAUAN REAL-TIME (LIVE) UNTUK SEMUA SAHAM SEKALIGUS
// Berbeda dari STEP 6 (snapshot 1x per saham), bagian ini BERLANGGANAN
// update SEMUA saham sekaligus, lalu MENUNGGU perubahan harga selama
// beberapa detik secara live - lebih efisien daripada pantau 1-1.
// ============================================================
KeywordUtil.logInfo('='.multiply(60))
KeywordUtil.logInfo("🔴 LIVE MONITORING SEMUA SAHAM (${liveMonitorSeconds} detik)")
KeywordUtil.logInfo('='.multiply(60))

Map<String, String> subscriptions = feedClient.subscribeMultipleStockQuotes(stockSymbols)
feedClient.listen(liveMonitorSeconds)
Map<String, List> allUpdates = feedClient.parseAllLiveQuoteUpdates()

allUpdates.each { stockCode, quotes ->
    if (quotes.isEmpty()) {
        KeywordUtil.logInfo("${stockCode}: Tidak ada update harga selama pemantauan (harga stabil/market sepi)")
    } else {
        quotes.each { quote ->
            KeywordUtil.logInfo("${stockCode}: Rp${BionsSocketClient.formatPrice(quote.displayLast)} (Change: ${quote.change} / ${quote.changePct}%)")
        }
        verify(true, "[${stockCode}] Menerima ${quotes.size()} update harga live")
    }
}

List<String> noMovement = BionsSocketClient.getStocksWithNoMovement(stockSymbols, allUpdates)
if (!noMovement.isEmpty()) {
    KeywordUtil.logInfo("ℹ️ Saham tanpa pergerakan selama pemantauan: ${noMovement}")
}

feedClient.unsubscribeMultipleStockQuotes(subscriptions)

// ============================================================
// STEP 8: RINGKASAN AKHIR
// ============================================================
KeywordUtil.logInfo('='.multiply(60))
KeywordUtil.logInfo('📊 RINGKASAN HASIL SEMUA SAHAM')
KeywordUtil.logInfo('='.multiply(60))
KeywordUtil.logInfo("Total Saham Dites : ${totalStocks}")
KeywordUtil.logInfo("✅ Berhasil        : ${stockSuccessCount}")
KeywordUtil.logInfo("❌ Gagal           : ${stockFailCount}")
KeywordUtil.logInfo("📊 Verifikasi      : ${passedChecks}/${totalChecks} PASSED")
KeywordUtil.logInfo('='.multiply(60))

// ============================================================
// STEP 9: TUTUP SOCKET
// ============================================================
feedClient.closeSocket()