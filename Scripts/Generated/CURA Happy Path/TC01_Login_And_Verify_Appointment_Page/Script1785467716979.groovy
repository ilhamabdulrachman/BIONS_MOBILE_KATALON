import com.kms.katalon.core.testobject.ConditionType
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

TestObject byId(String id) {
    TestObject testObject = new TestObject(id)
    testObject.addProperty('id', ConditionType.EQUALS, id)
    return testObject
}

TestObject byCss(String name, String css) {
    TestObject testObject = new TestObject(name)
    testObject.addProperty('css', ConditionType.EQUALS, css)
    return testObject
}

WebUI.openBrowser('')
WebUI.maximizeWindow()
WebUI.navigateToUrl('https://katalon-demo-cura.herokuapp.com/')

WebUI.verifyElementText(byCss('homeHeader', 'header h1'), 'CURA Healthcare Service')
WebUI.click(byId('btn-make-appointment'))

WebUI.setText(byId('txt-username'), 'John Doe')
WebUI.setText(byId('txt-password'), 'ThisIsNotAPassword')
WebUI.click(byId('btn-login'))

WebUI.verifyElementText(byCss('appointmentHeader', '#appointment h2'), 'Make Appointment')
WebUI.verifyElementPresent(byId('combo_facility'), 10)
WebUI.verifyElementPresent(byId('txt_visit_date'), 10)
WebUI.verifyElementPresent(byId('btn-book-appointment'), 10)

WebUI.closeBrowser()