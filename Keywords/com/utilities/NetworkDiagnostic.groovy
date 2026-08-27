package com.utilities

import com.kms.katalon.core.util.KeywordUtil
import groovy.json.JsonSlurper

/**
 * ============================================================
 * NETWORK DIAGNOSTIC - Membedakan penyebab kegagalan koneksi
 * ============================================================
 * Dipakai saat login/koneksi gagal, untuk membantu menentukan
 * apakah penyebabnya:
 *   1. Internet lambat/tidak stabil (di sisi client)
 *   2. Server BIONS tidak bisa dijangkau/down
 *   3. Server bisa dijangkau tapi port spesifik tertutup/lambat
 * Juga menyediakan info tambahan: ISP/provider yang dipakai,
 * dan estimasi kasar kecepatan download.
 * ============================================================
 */
class NetworkDiagnostic {

    /**
     * Jalankan diagnosa lengkap dan cetak hasilnya ke log.
     * Panggil ini di dalam blok catch saat login/koneksi gagal.
     *
     * @param targetHost host yang gagal diakses, misal "trade.bions.id"
     * @param targetPort port yang gagal diakses, misal 62229
     * @param includeSpeedTest kalau true, sekalian jalankan estimasi
     *        kecepatan download (menambah waktu beberapa detik)
     * @return Map berisi hasil diagnosa lengkap
     */
    static Map runDiagnostic(String targetHost, int targetPort, boolean includeSpeedTest = true) {
        KeywordUtil.logInfo('='.multiply(50))
        KeywordUtil.logInfo('🔍 MENJALANKAN DIAGNOSA JARINGAN...')
        KeywordUtil.logInfo('='.multiply(50))

        Map result = [
                internetOk        : false,
                internetLatencyMs : -1L,
                hostReachable     : false,
                hostLatencyMs     : -1L,
                portOpen          : false,
                portLatencyMs     : -1L,
                ispInfo           : null,
                downloadSpeedMbps : -1.0,
                diagnosis         : 'UNKNOWN',
        ]

        // ===== CEK 1: Internet Dasar (via Google DNS, selalu ada) =====
        result.internetLatencyMs = measureTcpLatency('8.8.8.8', 53, 3000)
        result.internetOk = result.internetLatencyMs >= 0

        if (result.internetOk) {
            KeywordUtil.logInfo("✅ Internet dasar: OK (${result.internetLatencyMs} ms)")
        } else {
            KeywordUtil.logInfo('❌ Internet dasar: GAGAL (tidak ada koneksi internet sama sekali)')
        }

        // ===== CEK 2: Info ISP/Provider (butuh internet, skip kalau internet mati) =====
        if (result.internetOk) {
            result.ispInfo = getIspInfo()
            if (result.ispInfo != null) {
                KeywordUtil.logInfo("📡 Provider   : ${result.ispInfo.isp}")
                KeywordUtil.logInfo("📍 Lokasi     : ${result.ispInfo.city}, ${result.ispInfo.region}, ${result.ispInfo.country}")
                KeywordUtil.logInfo("🌐 IP Publik  : ${result.ispInfo.ip}")
            } else {
                KeywordUtil.logInfo('⚠️ Info provider tidak dapat diambil (layanan pengecekan tidak merespons)')
            }
        }

        // ===== CEK 3: Estimasi Kecepatan Download (opsional, butuh waktu) =====
        if (result.internetOk && includeSpeedTest) {
            KeywordUtil.logInfo('⏳ Mengukur estimasi kecepatan download...')
            result.downloadSpeedMbps = estimateDownloadSpeedMbps()
            if (result.downloadSpeedMbps >= 0) {
                KeywordUtil.logInfo("📶 Estimasi kecepatan download: ${String.format('%.2f', result.downloadSpeedMbps)} Mbps")
            } else {
                KeywordUtil.logInfo('⚠️ Estimasi kecepatan download gagal diukur')
            }
        }

        // ===== CEK 4: Host Target Bisa Dijangkau =====
        result.hostLatencyMs = measureTcpLatency(targetHost, 443, 5000)
        result.hostReachable = result.hostLatencyMs >= 0

        if (result.hostReachable) {
            KeywordUtil.logInfo("✅ Host ${targetHost} dapat dijangkau (${result.hostLatencyMs} ms via port 443)")
        } else {
            KeywordUtil.logInfo("❌ Host ${targetHost} TIDAK dapat dijangkau sama sekali")
        }

        // ===== CEK 5: Port Spesifik yang Dituju =====
        result.portLatencyMs = measureTcpLatency(targetHost, targetPort, 5000)
        result.portOpen = result.portLatencyMs >= 0

        if (result.portOpen) {
            KeywordUtil.logInfo("✅ Port ${targetHost}:${targetPort} terbuka (${result.portLatencyMs} ms)")
        } else {
            KeywordUtil.logInfo("❌ Port ${targetHost}:${targetPort} TIDAK terbuka / timeout")
        }

        // ===== SIMPULKAN DIAGNOSA =====
        result.diagnosis = concludeDiagnosis(result)

        KeywordUtil.logInfo('='.multiply(50))
        KeywordUtil.logInfo("📋 KESIMPULAN: ${result.diagnosis}")
        KeywordUtil.logInfo('='.multiply(50))

        return result
    }

    /**
     * Ambil info ISP/provider dan lokasi geografis berdasarkan IP publik,
     * menggunakan layanan gratis ipinfo.io (tidak butuh API key untuk
     * pemakaian dasar, ada rate limit untuk pemakaian berlebihan).
     *
     * @return Map berisi: ip, isp, city, region, country. null kalau gagal.
     */
    static Map getIspInfo() {
        try {
            URL url = new URL('http://ip-api.com/json')
            HttpURLConnection conn = (HttpURLConnection) url.openConnection()
            conn.setConnectTimeout(5000)
            conn.setReadTimeout(5000)
            conn.setRequestMethod('GET')

            int responseCode = conn.getResponseCode()
            if (responseCode != 200) {
                return null
            }

            String responseBody = conn.getInputStream().getText('UTF-8')
            def json = new JsonSlurper().parseText(responseBody)

            return [
                    ip     : json.query ?: '-',
                    isp    : json.isp ?: '-',
                    city   : json.city ?: '-',
                    region : json.regionName ?: '-',
                    country: json.country ?: '-',
            ]
        } catch (Exception e) {
            KeywordUtil.logInfo("⚠️ Gagal ambil info ISP: ${e.message}")
            return null
        }
    }

    /**
     * Estimasi KASAR kecepatan download dengan mengunduh file test kecil
     * dari Cloudflare Speed Test endpoint dan mengukur waktunya.
     * CATATAN: Ini bukan speed test presisi tinggi seperti Speedtest.net,
     * cuma indikasi kasar untuk troubleshooting.
     *
     * @param testSizeBytes ukuran file test dalam bytes (default 2MB)
     * @param timeoutMs batas waktu maksimal
     * @return kecepatan dalam Mbps, atau -1 kalau gagal
     */
    static double estimateDownloadSpeedMbps(int testSizeBytes = 2_000_000, int timeoutMs = 10000) {
        try {
            String testUrl = "https://speed.cloudflare.com/__down?bytes=${testSizeBytes}"
            URL url = new URL(testUrl)
            HttpURLConnection conn = (HttpURLConnection) url.openConnection()
            conn.setConnectTimeout(timeoutMs)
            conn.setReadTimeout(timeoutMs)
            conn.setRequestMethod('GET')

            long start = System.currentTimeMillis()
            InputStream inputStream = conn.getInputStream()
            byte[] buffer = new byte[8192]
            long totalBytesRead = 0
            int bytesRead

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                totalBytesRead += bytesRead
            }
            inputStream.close()

            long elapsedMs = System.currentTimeMillis() - start
            if (elapsedMs <= 0 || totalBytesRead <= 0) {
                return -1.0
            }

            double bitsTransferred = totalBytesRead * 8.0
            double seconds = elapsedMs / 1000.0
            double mbps = (bitsTransferred / seconds) / 1_000_000.0

            return mbps
        } catch (Exception e) {
            KeywordUtil.logInfo("⚠️ Gagal mengukur kecepatan download: ${e.message}")
            return -1.0
        }
    }

    /**
     * Ukur waktu untuk membuka koneksi TCP ke host:port tertentu.
     *
     * @return waktu dalam ms kalau berhasil, -1 kalau gagal/timeout
     */
    private static long measureTcpLatency(String host, int port, int timeoutMs) {
        long start = System.currentTimeMillis()
        Socket socket = null
        try {
            socket = new Socket()
            socket.connect(new InetSocketAddress(host, port), timeoutMs)
            return System.currentTimeMillis() - start
        } catch (Exception ignored) {
            return -1L
        } finally {
            try {
                socket?.close()
            } catch (Exception ignored) {
                // abaikan
            }
        }
    }

    /**
     * Tentukan kesimpulan diagnosa berdasarkan kombinasi hasil pengecekan.
     */
    private static String concludeDiagnosis(Map result) {
        if (!result.internetOk) {
            return '🔴 INTERNET BERMASALAH - Tidak ada koneksi internet sama sekali. ' +
                    'Cek WiFi/koneksi data, atau hubungi admin jaringan.'
        }

        if (result.internetLatencyMs > 1000) {
            return '🟡 INTERNET LAMBAT - Koneksi internet ada tapi sangat lambat ' +
                    "(${result.internetLatencyMs} ms). Ini bisa menyebabkan timeout di socket."
        }

        if (result.downloadSpeedMbps >= 0 && result.downloadSpeedMbps < 1.0) {
            return '🟡 KECEPATAN INTERNET SANGAT RENDAH - Estimasi download cuma ' +
                    "${String.format('%.2f', result.downloadSpeedMbps)} Mbps. " +
                    'Ini bisa menyebabkan timeout/lambat di proses login.'
        }

        if (!result.hostReachable) {
            return '🔴 SERVER TIDAK DAPAT DIJANGKAU - Internet OK, tapi server BIONS ' +
                    'tidak bisa diakses. Kemungkinan: server down, DNS bermasalah, atau ' +
                    'perlu VPN/jaringan khusus untuk mengakses server ini.'
        }

        if (!result.portOpen) {
            return '🟠 PORT SPESIFIK TERTUTUP - Server BIONS bisa dijangkau (via HTTPS), ' +
                    'tapi port socket spesifik tidak merespons. Kemungkinan: service socket ' +
                    'sedang down, firewall memblokir port ini, atau port salah.'
        }

        if (result.portLatencyMs > 2000) {
            return '🟡 SERVER LAMBAT MERESPONS - Port terbuka tapi respons sangat lambat ' +
                    "(${result.portLatencyMs} ms). Kemungkinan server sedang overload."
        }

        return '🟢 JARINGAN & SERVER NORMAL - Semua pengecekan berhasil dengan baik. ' +
                'Kegagalan login kemungkinan BUKAN karena jaringan/server, ' +
                'melainkan soal lain (kredensial salah, sesi expired, dll).'
    }

    // ============================================================
    // DIAGNOSA JARINGAN DARI SISI HP ANDROID (via adb shell)
    // Berbeda dari diagnosa di atas yang cek jaringan Mac/komputer
    // ============================================================

    /**
     * Jalankan command shell di device Android via adb, dan return outputnya.
     * Membutuhkan Katalon Mobile Test Session yang sudah terhubung ke device,
     * ATAU adb tersedia di PATH sistem dan device sudah authorized.
     *
     * @param deviceId UDID device, misal "c965b1b0" (bisa dilihat dari `adb devices`)
     * @param shellCommand command yang dijalankan di dalam shell Android
     * @return output command sebagai String, atau null kalau gagal
     */
    static String runAdbShell(String deviceId, String shellCommand) {
        try {
            // Path lengkap ke adb, karena process.execute() di Java/Groovy
            // tidak selalu mewarisi PATH yang sama dengan shell Terminal
            String adbPath = '/Users/bionsrevamp/.katalon/tools/android_sdk/platform-tools/adb'

            List<String> command = [adbPath, '-s', deviceId, 'shell'] + shellCommand.split(' ').toList()
            Process process = command.execute()
            process.waitForOrKill(10000)
            String output = process.in.text.trim()
            String error = process.err.text.trim()

            if (output.isEmpty() && !error.isEmpty()) {
                KeywordUtil.logInfo("⚠️ ADB shell error: ${error}")
                return null
            }
            return output
        } catch (Exception e) {
            KeywordUtil.logInfo("⚠️ Gagal jalankan adb shell: ${e.message}")
            return null
        }
    }

    /**
     * Ambil info jaringan LANGSUNG DARI HP ANDROID (bukan dari Mac/komputer).
     * Mencakup: nama operator seluler, jenis koneksi (WiFi/Data Seluler),
     * dan IP address yang dipakai device.
     *
     * @param deviceId UDID device, misal "c965b1b0"
     * @return Map berisi info jaringan HP
     */
    static Map getDeviceNetworkInfo(String deviceId) {
        Map info = [
                operatorName : '-',
                connectionType: '-',
                wifiSsid     : '-',
                deviceIp     : '-',
        ]

        String operator = runAdbShell(deviceId, 'getprop gsm.operator.alpha')
        if (operator != null && !operator.isEmpty()) {
            info.operatorName = operator
        }

        String wifiState = runAdbShell(deviceId, 'settings get global wifi_on')
        String mobileDataState = runAdbShell(deviceId, 'settings get global mobile_data')

        if (wifiState == '1') {
            info.connectionType = 'WiFi'
            String ssid = runAdbShell(deviceId, 'dumpsys wifi | grep "mWifiInfo SSID"')
            if (ssid != null && !ssid.isEmpty()) {
                info.wifiSsid = ssid
            }
        } else if (mobileDataState == '1') {
            info.connectionType = 'Data Seluler'
        }

        String ipOutput = runAdbShell(deviceId, 'ip route')
        if (ipOutput != null && !ipOutput.isEmpty()) {
            def match = ipOutput =~ /src\s+(\d+\.\d+\.\d+\.\d+)/
            if (match.find()) {
                info.deviceIp = match.group(1)
            }
        }

        return info
    }

    /**
     * Cetak info jaringan HP Android ke log, dengan format yang rapi.
     * Panggil ini kapan saja kamu ingin tahu jaringan yang SEBENARNYA
     * dipakai oleh HP (bukan Mac/komputer yang menjalankan Katalon).
     *
     * @param deviceId UDID device
     */
    static void logDeviceNetworkInfo(String deviceId) {
        KeywordUtil.logInfo('='.multiply(50))
        KeywordUtil.logInfo('📱 INFO JARINGAN DI HP (BUKAN DI KOMPUTER)')
        KeywordUtil.logInfo('='.multiply(50))

        Map info = getDeviceNetworkInfo(deviceId)

        KeywordUtil.logInfo("📶 Jenis Koneksi : ${info.connectionType}")
        KeywordUtil.logInfo("📡 Operator SIM  : ${info.operatorName}")
        if (info.connectionType == 'WiFi') {
            KeywordUtil.logInfo("📶 WiFi SSID     : ${info.wifiSsid}")
        }
        KeywordUtil.logInfo("🌐 IP Device     : ${info.deviceIp}")

        // ===== ISP/IP Publik ASLI dari sudut pandang HP (bukan Mac) =====
        // CATATAN: Ini butuh 'curl' atau 'wget' tersedia di sistem Android
        // device. Tidak semua device/ROM punya tool ini terinstall - kalau
        // tidak ada, bagian ini akan gagal dengan pesan yang jelas.
        Map ispInfo = getDeviceIspInfo(deviceId)
        if (ispInfo != null) {
            KeywordUtil.logInfo("📡 ISP (dari HP) : ${ispInfo.isp}")
            KeywordUtil.logInfo("📍 Lokasi (HP)   : ${ispInfo.city}, ${ispInfo.region}, ${ispInfo.country}")
            KeywordUtil.logInfo("🌐 IP Publik (HP): ${ispInfo.ip}")
        } else {
            KeywordUtil.logInfo('⚠️ ISP/IP publik HP tidak dapat diambil (curl/wget mungkin tidak tersedia di device).')
        }

        // ===== Estimasi Kecepatan Download LANGSUNG DARI HP =====
        double deviceSpeed = estimateDeviceDownloadSpeedMbps(deviceId)
        if (deviceSpeed >= 0) {
            KeywordUtil.logInfo("📶 Kecepatan Download (HP): ${String.format('%.2f', deviceSpeed)} Mbps")
        } else {
            KeywordUtil.logInfo('⚠️ Kecepatan download HP tidak dapat diukur (curl/wget mungkin tidak tersedia di device).')
        }

        KeywordUtil.logInfo('='.multiply(50))
    }

    /**
     * Estimasi KASAR kecepatan download LANGSUNG DARI HP (bukan Mac),
     * dengan cara menyuruh HP download file test via 'adb shell curl/wget',
     * lalu ukur waktu eksekusinya dari sisi Mac (wall-clock time).
     *
     * KETERBATASAN: Sama seperti getDeviceIspInfo(), butuh curl/wget di
     * device. Juga, waktu yang diukur mencakup overhead komunikasi adb
     * itu sendiri, jadi ini estimasi KASAR, bukan pengukuran presisi tinggi.
     *
     * @param deviceId UDID device
     * @param testSizeBytes ukuran file test dalam bytes (default 2MB)
     * @param timeoutMs batas waktu maksimal
     * @return kecepatan dalam Mbps, atau -1 kalau gagal
     */
    static double estimateDeviceDownloadSpeedMbps(String deviceId, int testSizeBytes = 2_000_000, int timeoutMs = 15000) {
        String adbPath = '/Users/bionsrevamp/.katalon/tools/android_sdk/platform-tools/adb'
        String testUrl = "http://speed.cloudflare.com/__down?bytes=${testSizeBytes}"

        // Coba curl dulu (download ke /dev/null, tidak simpan file)
        double result = tryDeviceDownload(adbPath, deviceId, "curl -s -o /dev/null '${testUrl}'", testSizeBytes, timeoutMs)
        if (result >= 0) {
            return result
        }

        // Kalau curl gagal/tidak ada, coba wget
        return tryDeviceDownload(adbPath, deviceId, "wget -q -O /dev/null '${testUrl}'", testSizeBytes, timeoutMs)
    }

    /**
     * Helper internal: jalankan 1 percobaan download di device, ukur waktunya.
     */
    private static double tryDeviceDownload(String adbPath, String deviceId, String shellCommand, int testSizeBytes, int timeoutMs) {
        try {
            List<String> command = [adbPath, '-s', deviceId, 'shell', shellCommand]
            long start = System.currentTimeMillis()
            Process process = command.execute()
            process.waitForOrKill(timeoutMs)
            long elapsedMs = System.currentTimeMillis() - start

            if (process.exitValue() != 0 || elapsedMs <= 0) {
                return -1.0
            }

            double bitsTransferred = testSizeBytes * 8.0
            double seconds = elapsedMs / 1000.0
            return (bitsTransferred / seconds) / 1_000_000.0
        } catch (Exception e) {
            return -1.0
        }
    }

    /**
     * Ambil info ISP/IP publik ASLI yang terlihat dari koneksi HP (bukan Mac),
     * dengan cara menyuruh HP sendiri melakukan request ke ip-api.com lewat
     * 'adb shell curl' atau 'adb shell wget'.
     *
     * KETERBATASAN: Membutuhkan curl atau wget tersedia di sistem Android
     * device. Banyak device modern (Android 9+) sudah menyertakan toybox
     * yang punya utilitas dasar ini, tapi TIDAK DIJAMIN ada di semua device/ROM.
     *
     * @param deviceId UDID device
     * @return Map berisi: ip, isp, city, region, country. null kalau gagal.
     */
    static Map getDeviceIspInfo(String deviceId) {
        // Coba curl dulu
        String response = runAdbShellRaw(deviceId, 'curl -s http://ip-api.com/json')

        // Kalau curl tidak ada/gagal, coba wget
        if (response == null || response.isEmpty() || !response.trim().startsWith('{')) {
            response = runAdbShellRaw(deviceId, 'wget -qO- http://ip-api.com/json')
        }

        if (response == null || response.isEmpty() || !response.trim().startsWith('{')) {
            return null
        }

        try {
            def json = new JsonSlurper().parseText(response.trim())
            return [
                    ip     : json.query ?: '-',
                    isp    : json.isp ?: '-',
                    city   : json.city ?: '-',
                    region : json.regionName ?: '-',
                    country: json.country ?: '-',
            ]
        } catch (Exception e) {
            KeywordUtil.logInfo("⚠️ Gagal parse response ISP dari HP: ${e.message}")
            return null
        }
    }

    /**
     * Versi runAdbShell yang menerima command sebagai satu string utuh
     * (dengan spasi di dalamnya, misal untuk URL), tanpa split otomatis
     * per-spasi seperti runAdbShell() biasa.
     */
    private static String runAdbShellRaw(String deviceId, String fullShellCommand) {
        try {
            String adbPath = '/Users/bionsrevamp/.katalon/tools/android_sdk/platform-tools/adb'
            List<String> command = [adbPath, '-s', deviceId, 'shell', fullShellCommand]
            Process process = command.execute()
            process.waitForOrKill(8000)
            return process.in.text.trim()
        } catch (Exception e) {
            return null
        }
    }
}