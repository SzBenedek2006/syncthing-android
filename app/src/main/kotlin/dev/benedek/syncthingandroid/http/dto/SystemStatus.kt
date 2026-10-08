package dev.benedek.syncthingandroid.http.dto

/**
 * This contains the data returned by GET [dev.benedek.syncthingandroid.http.GetRequest.Companion.URI_SYSTEM] (`/rest/system/status` )
 * This is up to date as of Syncthing version 2.1
 *
 * TODO: Write docs for all properties
 * @property sys RAM used in bytes
 */
class SystemStatus {
	var alloc: Long = 0
	var connectionServiceStatus: Map<String, ConnectionServiceStatusEntry>? = null
	var cpuPercent: Double = 0.0
	var discoveryEnabled: Boolean = false

	@Deprecated(
		message = "Deprecated in Syncthing v1.18.0: use discoveryStatus instead.",
		replaceWith = ReplaceWith("discoveryStatus?.mapValues { it.value.error }?.filterValues { it != null }")
	)
	var discoveryErrors: MutableMap<String, String>? = null
	var discoveryStatus: Map<String, DiscoveryEntry>? = null

	@Deprecated(
		message = "Deprecated in Syncthing v1.18.0: use discoveryStatus.size instead.",
		replaceWith = ReplaceWith("discoveryStatus?.size ?: 0")
	)
	var discoveryMethods: Int = 0
	var goroutines: Int = 0
	var guiAddressOverridden: Boolean = false
	var guiAddressUsed: String? = null
	var lastDialStatus: Map<String, LastDialStatusEntry>? = null
	var myID: String? = null
	var pathSeparator: String? = null
	var startTime: String? = null
	var sys: Long = 0
	var tilde: String? = null
	var uptime: Long = 0
	var urVersionMax: Int = 0

	data class DiscoveryEntry(
		val error: String? = null
	)

	data class ConnectionServiceStatusEntry(
		val error: String? = null,
		val lanAddresses: List<String> = emptyList(),
		val wanAddresses: List<String> = emptyList()
	)

	data class LastDialStatusEntry(
		val `when`: String? = null, // TODO: Maybe solve the naming
		val error: String? = null
	)
}


/**
 * The official docs are seemingly very out of date or just wrong.
 *
 * Response according to docs:
 *
 * ```json
 * {
 *   "alloc": 30618136,
 *   "connectionServiceStatus": {
 *     "dynamic+https://relays.syncthing.net/endpoint": {
 *       "error": null,
 *       "lanAddresses": [
 *         "relay://23.92.71.120:443/?id=53STGR7-YBM6FCX-PAZ2RHM-YPY6OEJ-WYHVZO7-PCKQRCK-PZLTP7T-434XCAD&pingInterval=1m0s&networkTimeout=2m0s&sessionLimitBps=0&globalLimitBps=0&statusAddr=:22070&providedBy=canton7"
 *       ],
 *       "wanAddresses": [
 *         "relay://23.92.71.120:443/?id=53STGR7-YBM6FCX-PAZ2RHM-YPY6OEJ-WYHVZO7-PCKQRCK-PZLTP7T-434XCAD&pingInterval=1m0s&networkTimeout=2m0s&sessionLimitBps=0&globalLimitBps=0&statusAddr=:22070&providedBy=canton7"
 *       ]
 *     },
 *     "tcp://0.0.0.0:22000": {
 *       "error": null,
 *       "lanAddresses": [
 *         "tcp://0.0.0.0:22000"
 *       ],
 *       "wanAddresses": [
 *         "tcp://0.0.0.0:22000"
 *       ]
 *     }
 *   },
 *   "cpuPercent": 0,
 *   "discoveryEnabled": true,
 *   "discoveryErrors": {
 *     "global@https://discovery-v4-1.syncthing.net/v2/": "500 Internal Server Error",
 *     "global@https://discovery-v4-2.syncthing.net/v2/": "Post https://discovery-v4-2.syncthing.net/v2/: net/http: request canceled while waiting for connection (Client.Timeout exceeded while awaiting headers)",
 *     "global@https://discovery-v4-3.syncthing.net/v2/": "Post https://discovery-v4-3.syncthing.net/v2/: net/http: request canceled while waiting for connection (Client.Timeout exceeded while awaiting headers)",
 *     "global@https://discovery-v6-1.syncthing.net/v2/": "Post https://discovery-v6-1.syncthing.net/v2/: dial tcp [2001:470:28:4d6::5]:443: connect: no route to host",
 *     "global@https://discovery-v6-2.syncthing.net/v2/": "Post https://discovery-v6-2.syncthing.net/v2/: dial tcp [2604:a880:800:10::182:a001]:443: connect: no route to host",
 *     "global@https://discovery-v6-3.syncthing.net/v2/": "Post https://discovery-v6-3.syncthing.net/v2/: dial tcp [2400:6180:0:d0::d9:d001]:443: connect: no route to host"
 *   },
 *   "discoveryStatus": {
 *     "IPv4 local": {
 *       "error": null
 *     },
 *     "IPv6 local": {
 *       "error": null
 *     },
 *     "global@https://discovery-v4-1.syncthing.net/v2/": {
 *       "error": "500 Internal Server Error"
 *     },
 *     "global@https://discovery-v4-2.syncthing.net/v2/": {
 *       "error": "Post https://discovery-v4-2.syncthing.net/v2/: net/http: request canceled while waiting for connection (Client.Timeout exceeded while awaiting headers)"
 *     },
 *     "global@https://discovery-v4-3.syncthing.net/v2/": {
 *       "error": "Post https://discovery-v4-3.syncthing.net/v2/: net/http: request canceled while waiting for connection (Client.Timeout exceeded while awaiting headers)"
 *     },
 *     "global@https://discovery-v6-1.syncthing.net/v2/": {
 *       "error": "Post https://discovery-v6-1.syncthing.net/v2/: dial tcp [2001:470:28:4d6::5]:443: connect: no route to host"
 *     },
 *     "global@https://discovery-v6-2.syncthing.net/v2/": {
 *       "error": "Post https://discovery-v6-2.syncthing.net/v2/: dial tcp [2604:a880:800:10::182:a001]:443: connect: no route to host"
 *     },
 *     "global@https://discovery-v6-3.syncthing.net/v2/": {
 *       "error": "Post https://discovery-v6-3.syncthing.net/v2/: dial tcp [2400:6180:0:d0::d9:d001]:443: connect: no route to host"
 *     }
 *   },
 *   "discoveryMethods": 8,
 *   "goroutines": 49,
 *   "lastDialStatus": {
 *       "tcp://10.20.30.40": {
 *         "when": "2019-05-16T07:41:23Z",
 *         "error": "dial tcp 10.20.30.40:22000: i/o timeout"
 *       },
 *       "tcp://172.16.33.3:22000": {
 *         "when": "2019-05-16T07:40:43Z",
 *         "ok": true
 *       },
 *       "tcp://83.233.120.221:22000": {
 *         "when": "2019-05-16T07:41:13Z",
 *         "error": "dial tcp 83.233.120.221:22000: connect: connection refused"
 *       }
 *   },
 *   "myID": "P56IOI7-MZJNU2Y-IQGDREY-DM2MGTI-MGL3BXN-PQ6W5BM-TBBZ4TJ-XZWICQ2",
 *   "pathSeparator": "/",
 *   "startTime": "2016-06-06T19:41:43.039284753+02:00",
 *   "sys": 42092792,
 *   "themes": [
 *     "default",
 *     "dark"
 *   ],
 *   "tilde": "/Users/jb",
 *   "uptime": 2635
 * }
 * ```
 *
 * Actual response:
 * ```json
 * {
 *   "alloc": 21275616,
 *   "connectionServiceStatus": {
 *     "dynamic+https://relays.syncthing.net/endpoint": {
 *       "error": null,
 *       "lanAddresses": [
 *         "relay://208.87.129.98:22067/?id=HILMPXH-IBDLPH2-BOBSQPZ-LTON6N2-JWJH4P4-2SZE6G4-DNQCDO7-TNVSWAT"
 *       ],
 *       "wanAddresses": [
 *         "relay://208.87.129.98:22067/?id=HILMPXH-IBDLPH2-BOBSQPZ-LTON6N2-JWJH4P4-2SZE6G4-DNQCDO7-TNVSWAT"
 *       ]
 *     },
 *     "quic://0.0.0.0:22000": {
 *       "error": null,
 *       "lanAddresses": [
 *         "quic://0.0.0.0:22000",
 *         "quic://192.168.0.79:22000"
 *       ],
 *       "wanAddresses": [
 *         "quic://0.0.0.0:22000",
 *         "quic://84.0.171.111:22000",
 *         "quic://192.168.1.101:8822",
 *         "quic://0.0.0.0:8822",
 *         "quic://192.168.1.101:22826",
 *         "quic://0.0.0.0:22826"
 *       ]
 *     },
 *     "tcp://0.0.0.0:22000": {
 *       "error": null,
 *       "lanAddresses": [
 *         "tcp://0.0.0.0:22000",
 *         "tcp://192.168.0.79:22000"
 *       ],
 *       "wanAddresses": [
 *         "tcp://0.0.0.0:0",
 *         "tcp://0.0.0.0:22000",
 *         "tcp://192.168.1.101:8822",
 *         "tcp://0.0.0.0:8822",
 *         "tcp://192.168.1.101:22826",
 *         "tcp://0.0.0.0:22826"
 *       ]
 *     }
 *   },
 *   "cpuPercent": 0,
 *   "discoveryEnabled": true,
 *   "discoveryErrors": {},
 *   "discoveryMethods": 5,
 *   "discoveryStatus": {
 *     "IPv4 local": {
 *       "error": null
 *     },
 *     "IPv6 local": {
 *       "error": null
 *     },
 *     "global@https://discovery-announce-v4.syncthing.net/v2/": {
 *       "error": null
 *     },
 *     "global@https://discovery-announce-v6.syncthing.net/v2/": {
 *       "error": null
 *     },
 *     "global@https://discovery-lookup.syncthing.net/v2/": {
 *       "error": null
 *     }
 *   },
 *   "goroutines": 138,
 *   "guiAddressOverridden": false,
 *   "guiAddressUsed": "127.0.0.1:8384",
 *   "lastDialStatus": {
 *     "quic://10.172.40.135:22000": {
 *       "when": "2026-10-08T07:25:15Z",
 *       "error": "dial: timeout: no recent network activity"
 *     },
 *     "quic://37.76.16.151:63448": {
 *       "when": "2026-10-08T08:16:45Z",
 *       "error": null
 *     },
 *     "quic://[2a00:1110:140:1e42:400e:a950:1d75:c988]:22000": {
 *       "when": "2026-10-08T08:17:05Z",
 *       "error": null
 *     },
 *     "relay://94.231.0.91:22069/?id=NEEOLPT-BUC7GUW-5CZUC7Y-SXXA3VN-SMXJNNJ-T5IJ4XQ-NQBYB2M-OT3XPAD": {
 *       "when": "2026-10-07T10:22:08Z",
 *       "error": "incorrect response code 1: not found"
 *     },
 *     "tcp://10.173.184.8:22000": {
 *       "when": "2026-10-08T08:34:31Z",
 *       "error": "dial tcp 10.173.184.8:22000: i/o timeout"
 *     },
 *     "tcp://192.168.0.53:22000": {
 *       "when": "2026-10-07T18:40:32Z",
 *       "error": "dial tcp 192.168.0.53:22000: connect: no route to host"
 *     },
 *     "tcp://[fe80::ea2a:eaff:fe0d:970b%25wlan0]:29823": {
 *       "when": "2026-10-07T16:10:38Z",
 *       "error": "dial tcp [fe80::ea2a:eaff:fe0d:970b%wlan0]:29823: i/o timeout"
 *     }
 *   },
 *   "myID": "XNIMHJC-A6ETCY7-YVZEWOU-ADM7QON-XAKWXE6-X4XTNHH-ERTVFM5-NSU7PAP",
 *   "pathSeparator": "/",
 *   "startTime": "2026-10-07T10:30:20+02:00",
 *   "sys": 48412968,
 *   "tilde": "/home/szbenedek2006",
 *   "uptime": 86652,
 *   "urVersionMax": 3
 * }
 * ```
 *
 *
 */
