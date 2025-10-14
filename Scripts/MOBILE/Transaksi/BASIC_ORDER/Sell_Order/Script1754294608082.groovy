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

Mobile.startApplication('/Users/bionsrevamp/Downloads/app-development-profile 4.apk', false)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/LOGIN.PNG', FailureHandling.STOP_ON_FAILURE)

Mobile.tap(findTestObject('TEST_LOGIN/skip_onboarding'), 1)

Mobile.setText(findTestObject('TEST_LOGIN/user_id'), '1B029', 0)

Mobile.setText(findTestObject('TEST_LOGIN/pasword'), 'x', 0)

Mobile.setText(findTestObject('TEST_LOGIN/pin'), 'x12345', 0)

Mobile.tap(findTestObject('TEST_LOGIN/btn_'), 1)

//Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Login.PNG')
Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/DASHBOARD.PNG', FailureHandling.STOP_ON_FAILURE)

Mobile.delay(1, FailureHandling.STOP_ON_FAILURE)

Mobile.tap(findTestObject('TEST_LOGIN/SKIP_QUIK_TOUR'), 1)

Mobile.tap(findTestObject('Transaksi/button_buy_sell'), 1)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Basicorder.PNG')

Mobile.tap(findTestObject('Transaksi/Skip_basic_order'), 1)

Mobile.tap(findTestObject('Transaksi/Change_Stock'), 1)

Mobile.setText(findTestObject('Transaksi/input_stock'), 'ACES', 0)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/1Selectstock.PNG')

Mobile.tap(findTestObject('Transaksi/select_stock'), 1)

Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/Selectstock2.PNG')

//Mobile.tap(findTestObject('Transaksi/tab_price-'), 0, FailureHandling.STOP_ON_FAILURE)
//Mobile.tap(findTestObject('Transaksi/tab_plus_lot'), 1)
Mobile.takeScreenshot('/Users/bionsrevamp/Katalon Studio/Bions__/Reports/20250801_113059/Mobile/Login/2Basicorder5.PNG')

Mobile.tap(findTestObject('Transaksi/BTN_SELL'), 1)

Mobile.delay(5, FailureHandling.STOP_ON_FAILURE)

Mobile.swipe(500, 1500, 500, 500)

Mobile.tap(findTestObject('Transaksi/Sell_/Button_Sell'), 1)

Mobile.tap(findTestObject('Transaksi/Sell_/confirm_submit_sell'), 1)

Mobile.tap(findTestObject('Transaksi/Sell_/View_Order_List'), 1)

Mobile.tap(findTestObject('Transaksi/Sell_/Skip_SELL'), 1)

