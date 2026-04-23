import com.kms.katalon.core.mobile.keyword.MobileBuiltInKeywords as Mobile
import com.kms.katalon.core.mobile.keyword.internal.MobileDriverFactory
import io.appium.java_client.AppiumDriver

// ID Aplikasi BIONS
String appPackage = 'id.bions.bnis.android.v2'

// 1. Start Application (Gunakan false agar tidak install ulang terus)
Mobile.startApplication(appPackage, false)

// 2. Beri jeda agar Android 15 sempat loading
Mobile.delay(5)

// 3. Tes aksi sederhana (Input User ID)
// Pastikan nama Object 'User_id' sudah sesuai dengan yang kamu tangkap di Spy tadi
Mobile.setText(findTestObject('Login_firebase/User_id'), '1B029', 30)

// 4. Tutup aplikasi setelah selesai (Opsional)
// Mobile.closeApplication()