import static com.kms.katalon.core.checkpoint.CheckpointFactory.findCheckpoint
import static com.kms.katalon.core.testcase.TestCaseFactory.findTestCase
import static com.kms.katalon.core.testdata.TestDataFactory.findTestData
import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import static com.kms.katalon.core.testobject.ObjectRepository.findWindowsObject
import com.kms.katalon.core.checkpoint.Checkpoint as Checkpoint
import com.kms.katalon.core.cucumber.keyword.CucumberBuiltinKeywords as CucumberKW
import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords as Mobile
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
import com.utilities.SocketClient
import com.kms.katalon.core.util.KeywordUtil

// Data server dari konfigurasi yang Anda berikan
def feedServers = [
	"FEED_SERVER_1": "tcp://trade.bions.id:62229",
	"FEED_SERVER_2": "tcp://202.129.186.235:62229",
	"FEED_SERVER_3": "tcp://180.178.108.230:62229"
]

def tradingServers = [
	"TRADING_SERVER_1": "tcp://trade.bions.id:63339",
	"TRADING_SERVER_2": "tcp://202.129.186.235:63339",
	"TRADING_SERVER_3": "tcp://180.178.108.230:63339"
]

// Pesan yang akan dikirim (ganti dengan pesan yang valid)
def testMessage = "TEST_PING\n"

// Uji koneksi ke server Feed
boolean isFeedConnected = SocketClient.connectAndVerify(feedServers, testMessage)
if (isFeedConnected) {
	KeywordUtil.markPassed("Koneksi ke server Feed berhasil.")
} else {
	KeywordUtil.markFailed("Semua koneksi ke server Feed gagal.")
}

// Uji koneksi ke server Trading
boolean isTradingConnected = SocketClient.connectAndVerify(tradingServers, testMessage)
if (isTradingConnected) {
	KeywordUtil.markPassed("Koneksi ke server Trading berhasil.")
} else {
	KeywordUtil.markFailed("Semua koneksi ke server Trading gagal.")
}
