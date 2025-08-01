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
import com.kms.katalon.core.webui.driver.DriverFactory as DriverFactory

// Buka browser dan buka halaman login
WebUI.openBrowser('')

WebUI.navigateToUrl('https://webdeev.bions.id/login')

// Tunggu sampai elemen input User ID muncul (gunakan waktu tunggu dinamis)
WebUI.waitForElementPresent(findTestObject('WEB/LoginPage/txt_UserID', [('variable') : '']), 10)

// Input User ID
WebUI.setText(findTestObject('WEB/LoginPage/txt_UserID'), '1B029')

// Input Password
WebUI.setText(findTestObject('WEB/LoginPage/txt_Password'), 'x')

// Input PIN (asumsi ini input ke-3)
WebUI.setText(findTestObject('Object Repository/Page_Login/input_pin'), 'x12345')

// Klik tombol login / submit
WebUI.click(findTestObject('Object Repository/Page_Login/btn_login'))

// (Opsional) Klik elemen navigasi selanjutnya
WebUI.click(findTestObject('Object Repository/Page_Login/btn_nav'))

// Tutup browser
WebUI.closeBrowser()

