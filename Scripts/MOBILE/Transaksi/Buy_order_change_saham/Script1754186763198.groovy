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
import com.utilities.ShimmerWait as ShimmerWait
import groovy.json.JsonSlurper as JsonSlurper
import com.utilities.OrderVerification as OrderVerification
import java.math.BigDecimal as BigDecimal

String clientID = '1B029'

String stockCode = 'APLN'

BigDecimal orderPrice = new BigDecimal('171')

int lotAmount = 3

boolean isMarketOpen = CustomKeywords.'com.utilities.TradingHours.isMarketOpen'()

if (isMarketOpen) {
	KeywordUtil.logInfo('Bursa sedang buka. Melanjutkan pengujian...')
} else {
	boolean isMarketBreak = CustomKeywords.'com.utilities.TradingHours.isMarketBreak'()

	if (isMarketBreak) {
		KeywordUtil.markFailed('Tes gagal. Bursa sedang istirahat.', FailureHandling.STOP_ON_FAILURE)
	} else {
		KeywordUtil.markFailed('Tes gagal. Bursa sedang tutup.', FailureHandling.STOP_ON_FAILURE)
	}
}

//def elemenDashboard = findTestObject('TEST_LOGIN/SKIP_QUIK_TOUR')
//NetworkChecker.verifyInternetConnection()
Mobile.startApplication('/Users/bionsrevamp/Downloads/app-production-profile.apk', true)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/LOGIN.PNG', FailureHandling.STOP_ON_FAILURE)

//NetworkChecker.verifyInternetConnection()
Mobile.tap(findTestObject('TEST_LOGIN/skip_onboarding'), 0)

Mobile.setText(findTestObject('Login_firebase/User_id'), '23AA50456', 0)

Mobile.setText(findTestObject('Login_firebase/Pw'), 'kittiw222', 0)

Mobile.setText(findTestObject('Login_firebase/Pin'), 'kittiw333', 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Login0.PNG')

Mobile.tap(findTestObject('TEST_LOGIN/btn_'), 0)

//NetworkChecker.verifyInternetConnection()
Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Login1.PNG')

TcpClient client = new TcpClient()

client.connect('trade.bions.id', 62229 // FEED_SERVER_1
    )

// Kirim login
client.sendMessage('{ "action":"login", "user":"23AA50456", "password":"kittiw222" }')

// Listen 5 detik untuk capture response login
client.listen(5)

// 🔌 Tutup koneksi
client.close()

Mobile.tap(findTestObject('TEST_LOGIN/SKIP_QUIK_TOUR'), 0)

Mobile.tap(findTestObject('Transaksi/button_buy_sell'), 1)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Basicorder.PNG')

Mobile.tap(findTestObject('Transaksi/Skip_basic_order'), 1)

Mobile.tap(findTestObject('Transaksi/Change_Stock'), 1)

Mobile.setText(findTestObject('Transaksi/input_stock'), 'GOTO', 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Selectstock.PNG')

Mobile.tap(findTestObject('Transaksi/select_stock'), 1)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Selectstock1.PNG')

//Mobile.swipe(500, 1500, 500, 500)

Mobile.delay(5, FailureHandling.STOP_ON_FAILURE)

//Mobile.tap(findTestObject('Transaksi/tab_price-'), 0, FailureHandling.STOP_ON_FAILURE)
//Mobile.tap(findTestObject('Transaksi/tab_plus_lot'), 1)
Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/1Basicorder5.PNG')

Mobile.tap(findTestObject('Transaksi/BUTTON_BUY'), 1)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/1Basicorder2.PNG')

Mobile.tap(findTestObject('Transaksi/confirm_submit_buy'), 1)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/1Basicorder3.PNG')

Mobile.tap(findTestObject('Transaksi/view_order_list'), 1)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/1Orderlist.PNG')

Mobile.tap(findTestObject('Transaksi/Skip_quick_tour_orderlist'), 1)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/1Orderlista.PNG')

Mobile.closeApplication()

