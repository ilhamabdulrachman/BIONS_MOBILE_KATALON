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

String facility = 'Seoul CURA Healthcare Center'
String visitDate = '30/08/2026'
String comment = 'Happy path appointment created by automation.'

WebUI.openBrowser('')
WebUI.maximizeWindow()
WebUI.navigateToUrl('https://katalon-demo-cura.herokuapp.com/')

WebUI.click(byId('btn-make-appointment'))
WebUI.setText(byId('txt-username'), 'John Doe')
WebUI.setText(byId('txt-password'), 'ThisIsNotAPassword')
WebUI.click(byId('btn-login'))

WebUI.verifyElementText(byCss('appointmentHeader', '#appointment h2'), 'Make Appointment')
WebUI.selectOptionByValue(byId('combo_facility'), facility, false)
WebUI.click(byId('chk_hospotal_readmission'))
WebUI.click(byId('radio_program_medicaid'))
WebUI.setText(byId('txt_visit_date'), visitDate)
WebUI.setText(byId('txt_comment'), comment)
WebUI.click(byId('btn-book-appointment'))

WebUI.verifyElementText(byCss('confirmationHeader', '#summary h2'), 'Appointment Confirmation')
WebUI.verifyElementText(byId('facility'), facility)
WebUI.verifyElementText(byId('hospital_readmission'), 'Yes')
WebUI.verifyElementText(byId('program'), 'Medicaid')
WebUI.verifyElementText(byId('visit_date'), visitDate)
WebUI.verifyElementText(byId('comment'), comment)

WebUI.closeBrowser()