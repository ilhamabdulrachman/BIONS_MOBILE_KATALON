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
import java.util.ArrayList as ArrayList
import java.util.Map as Map
String clientID = '1B029'
String stockCode = 'APLN'
BigDecimal orderPrice = new BigDecimal('168')
int lotAmount = 5000        
String side = 'B'
List<String> expectedStatuses = ['Open', 'Partial', 'Match (Executed)', 'Withdraw (Cancelled)', 'Amend', 'Reject', 'Pending New'
    , 'Hold Booking', 'Booked']
List<String> expectedBoardID = ['RG']

int splitAmount = 10

List<Integer> expectedSplitLots =
        (1..splitAmount).collect { 1 }

//boolean isMarketOpen = CustomKeywords.'com.utilities.TradingHours.isMarketOpen'()
//if (isMarketOpen) {
//    KeywordUtil.logInfo('Bursa sedang buka. Melanjutkan pengujian login...')
//} else {
//    boolean isMarketBreak = CustomKeywords.'com.utilities.TradingHours.isMarketBreak'()
//    if (isMarketBreak) {
//        KeywordUtil.markFailed('Tes gagal. Bursa sedang istirahat.', FailureHandling.STOP_ON_FAILURE)
//    } else {
//        KeywordUtil.markFailed('Tes gagal. Bursa sedang tutup.', FailureHandling.STOP_ON_FAILURE)
//    }
//}
String applicationID = 'id.bions.bnis.android.v2'

try {
    Mobile.startExistingApplication(applicationID, FailureHandling.STOP_ON_FAILURE)

    KeywordUtil.logInfo("✅ Aplikasi dengan ID '$applicationID' berhasil diluncurkan.")
}
catch (Exception e) {
    KeywordUtil.markFailed('❌ Gagal meluncurkan aplikasi. Pastikan aplikasi sudah terinstal di perangkat. Error: ' + e.getMessage(), 
        FailureHandling.STOP_ON_FAILURE)
} 
CustomKeywords.'com.utilities.AppHealth.verifyAppIsAlive'()

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/LOGIN.PNG', FailureHandling.STOP_ON_FAILURE)

Mobile.setText(findTestObject('Login_firebase/User_id'), clientID, 0)

Mobile.setText(findTestObject('Login_firebase/Pw'), 'q', 0)

Mobile.setText(findTestObject('TEST_LOGIN/Pin2'), 'q12345', 0)

def start = Instant.now()

Mobile.tap(findTestObject('TEST_LOGIN/btn_'), 1)
boolean unhealthy =
    CustomKeywords.'com.utilities.FreezeDetector.detectFrozenOrCrashedScreenObserver'(5, 2)

if (unhealthy) {
    KeywordUtil.logWarning("⚠️ App freeze / crash terdeteksi")
}


def end = Instant.now()

def seconds = Duration.between(start, end).toMillis() / 1000

KeywordUtil.logInfo("⏱️ Waktu login sampai dashboard: $seconds detik")

def now = ZonedDateTime.now(ZoneId.of('Asia/Jakarta'))

def fmt = DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss')

KeywordUtil.logInfo('Login successful at ' + now.format(fmt))

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/DASHBOARD.PNG', FailureHandling.STOP_ON_FAILURE)

Mobile.delay(5, FailureHandling.STOP_ON_FAILURE)

Mobile.tap(findTestObject('Transaksi/BUYSELL'), 1)

Mobile.tap(findTestObject('Transaksi/CHANGE'), 0)

Mobile.setText(findTestObject('Transaksi/Sell_/enter_stock_name'), stockCode, 0)

Mobile.tap(findTestObject('Transaksi/TAP_STOCK_NAME'), 0)

Mobile.delay(8, FailureHandling.STOP_ON_FAILURE)

Mobile.setText(findTestObject('HNWI/split_order - 0'), splitAmount.toString(), 0) 
Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/ORDERAA.PNG')
CustomKeywords.'com.utilities.AppHealth.verifyAppIsAlive'()

Mobile.delay(3, FailureHandling.STOP_ON_FAILURE)

Mobile.tap(findTestObject('HNWI/button_HNWI'), 0)
CustomKeywords.'com.utilities.FreezeDetector.detectFrozenScreen'(
	5,
	2)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/ORDERA.PNG')

start = Instant.now()

Mobile.tap(findTestObject('HNWI/CONFIRM_HNWI'), 0)
CustomKeywords.'com.utilities.FreezeDetector.detectFrozenScreen'(
	5,
	2)
CustomKeywords.'com.utilities.AppHealth.verifyAppIsAlive'(
	'id.bions.bnis.android.v2')

end = Instant.now()

seconds = (Duration.between(start, end).toMillis() / 1000)

KeywordUtil.logInfo("⏱️ Waktu kirim split order sampai konfirmasi: $seconds detik")

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/ORDERD.PNG')


def client = new TcpClient()
client.connect('192.168.19.61', 62229)
client.sendMessage("{\"action\":\"login\", \"user\":\"${clientID}\", \"password\":\"q12345\"}")
client.listen(5)
client.sendMessage("{\"action\":\"subscribe\", \"channel\":\"order\", \"user\":\"${clientID}\"}")
KeywordUtil.logInfo("Menunggu ${splitAmount} respon order melalui FIX/Trading Engine...")
client.listen(15) 
client.close()

start1 = Instant.now()
Mobile.tap(findTestObject('Transaksi/view_order_list'), 1)
end1 = Instant.now()
seconds = (Duration.between(start1, end1).toMillis() / 1000)
KeywordUtil.markPassed("⏱️ Order List terbuka dalam $seconds detik")

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Orderlist3.PNG')
Mobile.delay(2, FailureHandling.STOP_ON_FAILURE)
Mobile.swipe(500, 1500, 500, 500)
Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Orderlist3.PNG')
Mobile.delay(2, FailureHandling.STOP_ON_FAILURE)
Mobile.swipe(500, 1500, 500, 500)
Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Orderlist3.PNG')
Mobile.delay(2, FailureHandling.STOP_ON_FAILURE)
Mobile.swipe(500, 1500, 500, 500)
Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/ORDERF.PNG')

KeywordUtil.logInfo("Memulai verifikasi database untuk ${splitAmount} entry order client ID $clientID...")

boolean dbVerificationResult = CustomKeywords.'com.utilities.OrderVerification.verifyLatestVariedSplitOrders'(
        clientID,
        stockCode,
        expectedSplitLots,
        orderPrice,
		expectedStatuses,
        side,
        expectedBoardID
    )

if (dbVerificationResult) {
    KeywordUtil.logInfo("✅ KESIMPULAN: Seluruh ${splitAmount} entry Split Order (dengan lot bervariasi) berhasil dikirim dan diverifikasi di Database Oracle.")
} else {
    KeywordUtil.logError('❌ KESIMPULAN: Ketidaksesuaian data order ditemukan di database untuk skenario lot bervariasi.')
}

Mobile.closeApplication()
