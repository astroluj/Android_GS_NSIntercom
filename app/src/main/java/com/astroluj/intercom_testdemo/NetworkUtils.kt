package com.astroluj.intercom_testdemo

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Patterns
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.reflect.Method
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.regex.Pattern


class NetworkUtils {
    companion object {
        const val IP = "ipAddress"
        const val MASK = "subnetMast"
        const val GATEWAY = "gateway"
        const val DNS_1 = "dns1"
        const val DNS_2 = "dns2"

        @JvmStatic
        fun getNetwork(context: Context): HashMap<String, String> {
            val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo = HashMap<String, String>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                manager.activeNetwork?.let {
                    val active = manager.getNetworkCapabilities(it)
                    // ethernet network
                    active?.let { active ->
                        if(active.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                            for (address in manager.getLinkProperties(it)!!.linkAddresses) {
                                if (!address.address.isLoopbackAddress && address.address.hostAddress.contains('.')) {
                                    // ip 넣기
                                    networkInfo[IP] = (address.address.hostAddress)
                                    // subnet mask 넣기
                                    networkInfo[MASK] = getSubnetMask(address.prefixLength)
                                }
                            }
                            // gateway 넣기
                            for (address in manager.getLinkProperties(it)!!.routes) {
                                address.gateway?.let { inetAddress ->
                                    if (inetAddress.hostAddress != "0.0.0.0." && !inetAddress.hostAddress.contains(':')) networkInfo[GATEWAY] = (inetAddress.hostAddress)
                                }
                            }
                            // dns 넣기
                            for (address in manager.getLinkProperties(it)!!.dnsServers) {
                                if (networkInfo[DNS_1].isNullOrEmpty()) networkInfo[DNS_1] = (address.hostAddress)
                                else networkInfo[DNS_2] = address.hostAddress
                            }
                        }
                        // wifi network
                        else if (active.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                            networkInfo.putAll(getWifiNetwork(context))
                        }
                    }
                }
            }
            else {
                // ethernet network
                @Suppress("DEPRECATION")
                manager.activeNetworkInfo?.let {
                    if (it.type == ConnectivityManager.TYPE_ETHERNET) {
                        val ipPrefix = ("((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(25[0-5]|2[0-4]"
                                + "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]"
                                + "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
                                + "|[1-9][0-9]|[0-9])\\/\\d{0,2})")
                        val routeCmd = "ip route"

                        // $ ip route
                        val process = Runtime.getRuntime().exec(routeCmd)
                        val br = BufferedReader(InputStreamReader(process.inputStream))
                        var line: String?
                        var res = ""
                        while (br.readLine().also { line = it } != null) {
                            res += line
                        }

                        if (res.contains("eth0")) {
                            var matcher = Patterns.IP_ADDRESS.matcher(res)
                            // ip 넣기
                            var ip = ""
                            while (matcher.find()) ip = matcher.group()
                            networkInfo[IP] = ip

                            // subnet mask 넣기
                            matcher = Pattern.compile(ipPrefix).matcher(res)
                            var prefixLength = ""
                            var gateway = ""
                            while (matcher.find()) {
                                val route = matcher.group().split("/")
                                gateway = route[0]
                                prefixLength = route[1]
                            }

                            networkInfo[MASK] = getSubnetMask(prefixLength.toInt())

                            // gateway 넣기
                            val lastNumIdx = gateway.lastIndexOf(".") + 1
                            gateway = gateway.substring(0, lastNumIdx).plus("0")
                            networkInfo[GATEWAY] = gateway
                        }
                        else return networkInfo

                        // get dns
                        val systemProperties = Class.forName("android.os.SystemProperties")
                        val method: Method = systemProperties.getMethod("get", String::class.java)
                        for (name in arrayOf("net.dns1", "net.dns2").withIndex()) {
                            val value = method.invoke(null, name.value) as String
                            if (value.isNotEmpty() && !networkInfo.contains(value)) {
                                when (name.index) {
                                    0 -> networkInfo[DNS_1] = value
                                    1 -> networkInfo[DNS_2] = value
                                }
                            }
                        }
                    }
                    // wifi network
                    else if (it.type == ConnectivityManager.TYPE_WIFI) {
                        networkInfo.putAll(getWifiNetwork(context))
                    }
                }
            }

            return networkInfo
        }

        /*
         * get IP address
         */
        private fun getNetworkInfo(): String {
            // Device에 있는 모든 네트워크에 대해 뺑뺑이를 돕니다.
            val networks = NetworkInterface.getNetworkInterfaces()
            for (network in networks) {
                //네트워크 중에서 IP가 할당된 넘들에 대해서 뺑뺑이를 한 번 더 돕니다.
                val netInterfaces = network.inetAddresses
                for (net in netInterfaces) {
                    //네트워크에는 항상 Localhost 즉, 루프백(LoopBack)주소가 있으며, 우리가 원하는 것이 아닙니다.
                    //IP는 IPv6와 IPv4가 있습니다.
                    //IPv6의 형태 : fe80::64b9::c8dd:7003
                    //IPv4의 형태 : 123.234.123.123
                    if (!net.isLoopbackAddress) {
                        if (net is Inet4Address) {
                            return net.hostAddress
                        }
                    }
                }
            }

            return ""
        }

        private fun getWifiNetwork(context: Context): HashMap<String, String> {
            val networkInfo = HashMap<String, String>()
            val dhcpInfo = (context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).dhcpInfo
            // ip
            networkInfo[IP] = getWifiAddress(dhcpInfo.ipAddress)
            // mask
            networkInfo[MASK] = getWifiAddress(dhcpInfo.netmask)
            // gateway
            networkInfo[GATEWAY] = getWifiAddress(dhcpInfo.gateway)
            // dns
            networkInfo[DNS_1] = getWifiAddress(dhcpInfo.dns1)
            networkInfo[DNS_2] = getWifiAddress(dhcpInfo.dns2)

            return networkInfo
        }

        private fun getWifiAddress(addr: Int): String {
            return String.format("%d.%d.%d.%d", addr and 0xFF, addr.shr(8) and 0xFF , addr.shr(16) and 0xFF, addr.shr(24) and 0xFF)
        }

        private fun getSubnetMask(prefixLength: Int): String {
            val mask = 0xFFFFFFFF.shl(32 - prefixLength)
            return String.format("%d.%d.%d.%d",
                    mask.shr(24) and 0xFF,
                    mask.shr(16) and 0xFF,
                    mask.shr(8) and 0xFF, mask and 0xFF)
        }
    }
}