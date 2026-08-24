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
import com.kms.katalon.core.util.KeywordUtil as KeywordUtil
import java.time.ZonedDateTime as ZonedDateTime
import java.time.ZoneId as ZoneId
import java.time.format.DateTimeFormatter as DateTimeFormatter
import java.time.Instant as Instant
import java.time.Duration as Duration
import com.utilities.TradingHours as TradingHours
import com.utilities.ShimmerWait as ShimmerWait
import groovy.json.JsonSlurper as JsonSlurper
import com.utilities.BionsSocketClient as BionsSocketClient

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

List<String> stockSymbols = ['BBNIRG', 'BBCARG',]

int totalChecks = 0

int passedChecks = 0

def verify = { boolean condition, String description ->
    totalChecks++

    if (condition) {
        passedChecks++

        KeywordUtil.logInfo("✅ PASS: $description")
    } else {
        KeywordUtil.markWarning("❌ FAIL: $description")
    }
}

// ============================================================
// STEP 1: LAUNCH APPLICATION
// ============================================================
try {
    Mobile.startExistingApplication(applicationID, FailureHandling.STOP_ON_FAILURE)

    KeywordUtil.logInfo("Aplikasi dengan ID '$applicationID' berhasil diluncurkan.")
}
catch (Exception e) {
    KeywordUtil.markFailed('Gagal meluncurkan aplikasi. Pastikan aplikasi sudah terinstal di perangkat. Error: ' + e.getMessage(), 
        FailureHandling.STOP_ON_FAILURE)
} 

// ============================================================
// STEP 2: LOGIN VIA UI MOBILE
// ============================================================
Mobile.setText(findTestObject('Login_firebase/User_id'), userId, 0)

Mobile.setText(findTestObject('Login_firebase/Pw'), password, 0)

Mobile.setText(findTestObject('Login_firebase/Pin'), pin, 0)

Mobile.takeScreenshot("$screenshotBasePath/Login0.PNG")

Instant start = Instant.now()

Mobile.tap(findTestObject('Login_V2/login'), 0)

Mobile.takeScreenshot("$screenshotBasePath/Login1.PNG")

Instant end = Instant.now()

double loginSeconds = Duration.between(start, end).toMillis() / 1000.0

KeywordUtil.logInfo("Waktu login sampai dashboard: $loginSeconds detik")

def now = ZonedDateTime.now(ZoneId.of('Asia/Jakarta'))

def fmt = DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss')

KeywordUtil.logInfo('Login successful at ' + now.format(fmt))

Mobile.takeScreenshot("$screenshotBasePath/Login_Biometric.PNG")

Mobile.tap(findTestObject('Login_firebase/Not_now'), 0)

Mobile.tap(findTestObject('NavBar_Scalability/trade'), 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Trade_V2.PNG')

Mobile.swipe(500, 1500, 500, 500)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Trade_V1.PNG')

Mobile.tap(findTestObject('Order_Book/Tap_Search'), 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Trade_V3.PNG')

Mobile.setText(findTestObject('Order_Book/find any stock'), 'BBCA', 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Trade_V2.PNG')

Mobile.tap(findTestObject('Order_Book/Select_Stock'), 0)

Mobile.closeApplication()

// CATATAN: Semua interaksi UI SUDAH SELESAI di titik ini. Socket login
// di bawah kemungkinan membuat sesi UI ter-logout (kebijakan
// single-session server) - ini sudah diperhitungkan dalam desain ini.
// ============================================================
// STEP 3: SOCKET FEED LOGIN + VERIFIKASI
// ============================================================
BionsSocketClient feedClient = new BionsSocketClient()

Map feedResult = feedClient.loginFeed(host, feedPort, userId, password, clientIp)

verify(feedResult.success == true, 'Feed Login berhasil (success = true)')

verify(feedResult.gatewayId != null, 'Feed Login mengembalikan Gateway ID yang valid')

KeywordUtil.logInfo("Feed Gateway: $feedResult.gatewayId, AutoRenew: $feedResult.autoRenew")

// ============================================================
// STEP 4: ORDERBOOK SNAPSHOT + TABEL + VERIFIKASI
// ============================================================
stockSymbols.each { stockSymbol ->
    KeywordUtil.logInfo('='.multiply(60))
    KeywordUtil.logInfo("📈 DATA SOCKET UNTUK: ${stockSymbol}")
    KeywordUtil.logInfo('='.multiply(60))

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

        long totalQueueBid = bids.sum { it.orderCount instanceof Number ? it.orderCount.longValue() : 0 } as long
        long totalLotBid = bids.sum { it.lot instanceof Number ? it.lot.longValue() : 0 } as long
        long totalLotOffer = offers.sum { it.lot instanceof Number ? it.lot.longValue() : 0 } as long
        long totalQueueOffer = offers.sum { it.orderCount instanceof Number ? it.orderCount.longValue() : 0 } as long

        KeywordUtil.logInfo('-'.multiply(50))
        KeywordUtil.logInfo("Total Queue Bid   : ${totalQueueBid}")
        KeywordUtil.logInfo("Total Lot Bid     : ${totalLotBid}")
        KeywordUtil.logInfo("Total Lot Offer   : ${totalLotOffer}")
        KeywordUtil.logInfo("Total Queue Offer : ${totalQueueOffer}")
        KeywordUtil.logInfo('-'.multiply(50))

        verify(bids.size() > 0, "[${stockSymbol}] Minimal 1 level Bid tersedia")
        verify(offers.size() > 0, "[${stockSymbol}] Minimal 1 level Offer tersedia")

        if (bids.size() > 0 && offers.size() > 0) {
            BigDecimal bestBidPrice = bids[0].price as BigDecimal
            BigDecimal bestOfferPrice = offers[0].price as BigDecimal
            verify(bestBidPrice < bestOfferPrice, "[${stockSymbol}] Best Bid (${bestBidPrice}) < Best Offer (${bestOfferPrice})")
        }
    } else {
        KeywordUtil.logInfo("Tidak ada data Orderbook untuk ${stockSymbol}")
    }
}

// ============================================================
// STEP 6: RINGKASAN VERIFIKASI
// ============================================================
KeywordUtil.logInfo('='.multiply(60))

KeywordUtil.logInfo("📊 RINGKASAN VERIFIKASI: $passedChecks/$totalChecks PASSED")

String stockList = stockSymbols.collect { it.toString() }.join(', ')
KeywordUtil.logInfo("   Saham yang diproses: ${stockList}")

KeywordUtil.logInfo('='.multiply(60))

// ============================================================
// STEP 6: TUTUP SOCKET & APLIKASI
// ============================================================
feedClient.closeSocket()

