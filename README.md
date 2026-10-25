# Falak VPN

**A real Android VPN client with a userspace TCP/IP stack — not a "fake VPN" that only flips a flag.**

[العربية](README_AR.md) · English · [Releases](#download)

Falak VPN is an open-source Android VPN client (Kotlin + Jetpack Compose). It creates a real
`VpnService` tunnel on the device and terminates every TCP connection the device opens **inside the
app's own userspace TCP/IP stack**, then relays the byte stream through an upstream proxy server
(VLESS, Trojan, SOCKS5 or HTTP CONNECT).

Everything the phone sends — YouTube, Google, every app on the device — travels through the tunnel.

---

## Why this project exists

Most "VPN" apps for Android either:

* ship a precompiled binary blob (`libv2ray.so`, `libtun2socks.so`) that you cannot audit, or
* fake the connection indicator while traffic quietly leaks, or
* depend on a closed-source "clash core" that is hundreds of megabytes.

Falak VPN does the hard part **from scratch, in readable Kotlin you can read in an afternoon**:
IPv4 parsing and checksums, the TCP state machine, segment retransmission, window handling,
fake-IP DNS interception, and the four upstream proxy protocols. There is no native code and no
obfuscated blob in the dependency tree — the only `.so` files in the APK come from AndroidX
DataStore.

---

## How it works

```
   Phone apps (YouTube, Chrome, ...)
            │
            ▼
   Android kernel ──► VpnService tun interface (MTU 1500)
            │                     │
            │            ┌────────▼────────┐
            │            │  TcpStack       │  ← userspace TCP/IP
            │            │  · handshake    │
            │            │  · seq/ack      │    (core module, pure Kotlin)
            │            │  · window       │
            │            │  · retransmit   │
            │            │  · checksums    │
            │            └────────┬────────┘
            │                     │
            │            ┌────────▼────────┐
            │            │  DnsResolver    │  ← fake-IP DNS
            │            │  198.18.0.0/15  │
            │            └────────┬────────┘
            │                     │
            ▼                     ▼
   ← packets back    ┌────────────────────────┐
                     │  ProxyClient           │
                     │  VLESS / Trojan /      │
                     │  SOCKS5 / HTTP CONNECT │
                     └───────────┬────────────┘
                                 ▼
                          Upstream server (yours)
```

### The fake-IP trick

When the phone asks "what is the IP of `www.youtube.com`?", the app does **not** let the local
resolver answer — local DNS is exactly what gets poisoned or blocked in censored networks. Instead
it replies with a **fake address** from `198.18.0.0/15` (the RFC 2544 benchmark range) and keeps a
reverse map.

When the phone then opens a TCP connection to `198.18.x.y:443`, the stack looks the address up,
recovers the real hostname, and asks the **upstream server** to resolve it (remote DNS). The result:
real hostnames are resolved by the server, not by your network.

---

## Features

| | |
|---|---|
| **Real tunnel** | Full `VpnService` + userspace TCP/IP stack written from scratch |
| **Protocols** | VLESS (TLS / REALITY / WebSocket), Trojan, SOCKS5, HTTP CONNECT |
| **Server manager** | Unlimited profiles, add/edit, share links, one-tap import of `vless://` `trojan://` `socks://` `http://` |
| **Server test** | ⚡ measures real **TCP → TLS → HTTP** through the tunnel and reports latency |
| **Per-app firewall** | Exclude banking / streaming apps from the tunnel, individually |
| **Kill switch** | Block all traffic outside the tunnel (Android 10+) |
| **Fake-IP DNS** | Names resolved by the upstream server, immune to local DNS poisoning |
| **In-app browser** | Built-in WebView for quick checks; traffic rides the tunnel like everything else |
| **Live statistics** | Real upload/download byte counters and active-connection count |
| **Material 3 UI** | Dark/light theme, Jetpack Compose, English + Arabic |
| **No native code** | Pure Kotlin. Only AndroidX DataStore contributes a `.so` |
| **No telemetry** | Nothing leaves the device except your own tunnel traffic |

---

## Download

Builds are produced by the workflow in `.github/workflows/android.yml` and attached to the
repository's releases.

<details>
<summary>Install from source</summary>

```bash
git clone https://github.com/YOUSSEFJEDIDI89/FalakVPN.git
cd FalakVPN

# point the build at your SDK
echo "sdk.dir=/path/to/Android/sdk" > local.properties

# create a signing key (never commit this)
keytool -genkeypair -v -keystore falak-release.jks -alias falak \
        -keyalg RSA -keysize 2048 -validity 10000

./gradlew :app:assembleRelease
# -> app/build/outputs/apk/release/app-release.apk
```

</details>

### Installing the APK

Android will ask you to allow **"install from unknown apps"** for the app you open the file with
(Files / Chrome / ZArchiver). Grant it, install, then open **Falak VPN**.

---

## First run — you need a server

**Falak VPN is an engine, not an internet provider.** Like every VPN client, it needs an upstream
proxy server to reach the internet through. Any of these work:

* a **VPS** you rent (e.g. a cheap Hetzner/DigitalOcean/Linode box) with a VLESS, Trojan or SOCKS5
  server installed,
* a **subscription link** from a provider you trust,
* your **own** proxy server.

Then:

1. **Servers** tab → **Add**, or paste a `vless://` / `trojan://` / `socks://` / `http://` link
   (multiple links at once are imported together).
2. Tap **⚡** next to the server — the app opens a real connection, negotiates TLS inside the tunnel
   and issues an HTTP request, then reports `OK · tcp 84ms · tls 120ms · HTTP 204`.
3. **Home** tab → tap the big circle to **connect**. Android asks for the VPN permission once.

> If the test says `Failed: timed out`, the server is unreachable from your network — try another
> one before blaming the app.

---

## Project layout

```
FalakVPN/
├── core/                              ← pure Kotlin/JVM, no Android dependencies
│   └── src/main/kotlin/app/falak/core/
│       ├── Ip.kt                      IPv4/TCP/UDP parsing + packet construction + checksums
│       ├── TcpStack.kt                the userspace TCP state machine
│       ├── Proxy.kt                   VLESS / Trojan / SOCKS5 / HTTP CONNECT clients
│       ├── DnsResolver.kt             fake-IP allocation + DNS response synthesis
│       ├── ServerTester.kt            end-to-end latency probe
│       └── Bytes.kt                   byte/checksum helpers
├── app/                               ← the Android application
│   └── src/main/kotlin/app/falak/vpn/
│       ├── FalakVpnService.kt         VpnService, tun loop, per-app firewall
│       ├── MainActivity.kt
│       ├── data/                      profile storage, link parser, firewall
│       └── ui/                        Compose screens (home, servers, apps, browser)
└── .github/workflows/android.yml      CI: build + test + release APK
```

The `core` module has **no Android dependencies at all**. That is what makes the interesting parts
testable on a plain JVM — see below.

---

## Testing

The engine is verified against **real sockets**, not mocks:

```bash
./gradlew :core:test
```

| Test | What it proves |
|---|---|
| `socks5 client tunnels bytes to a real destination` | SOCKS5 handshake against a live local server, byte-exact relay |
| `socks5 client authenticates when credentials are supplied` | username/password auth path |
| `http connect proxy tunnels bytes to a real destination` | HTTP `CONNECT` proxying |
| `ipv4 checksum matches an independent implementation` | our checksums vs. a separate RFC 1071 verifier |
| `device handshake completes and payload round trips through the tunnel` | a simulated **device kernel** completes a real TCP handshake with the stack and data flows both ways |
| `real tls session to youtube travels through the tunnel` | **a full TLS handshake to `www.youtube.com` and a real HTTPS response (≈200 KB) travel through the stack** |

The last one is the important one: it drives the tunnel with a synthetic device, performs a genuine
TLS negotiation inside it and asserts on the actual HTTP response that comes back.

### Bugs this test suite caught

Building the tests first paid off — these are all real defects that were found and fixed:

* **An infinite ACK storm** between the device kernel and the tunnel (pure ACKs were being
  "acknowledged" back). This would have burned battery and CPU continuously on a real phone.
* **Dropped payloads on retransmission** — the retry path re-sent headers only, corrupting the
  stream.
* **A SYN that consumed no sequence number**, so the first data segment overlapped the handshake.
* **A routing loop**: the tunnel's own upstream connection was being captured by the tunnel. Fixed
  with `VpnService.protect()` on the upstream socket.
* **A crash on Android 14+**: the `specialUse` foreground-service type requires the
  `FOREGROUND_SERVICE_SPECIAL_USE` permission, which was missing.
* **Local DNS resolution of the upstream hostname**, which breaks in exactly the networks this app
  is meant to work in. Now resolved before the tunnel takes over routing.

---

## How the pieces fit together

### `Ip.kt`
Parses incoming IPv4 packets and builds outgoing ones. Checksums are computed with the standard
RFC 1071 one's-complement algorithm; the test suite verifies them against an independent
implementation rather than against itself.

### `TcpStack.kt`
The heart of the project. It acts as the **server side** of every connection the device opens:

* completes the three-way handshake on the device's behalf,
* maintains sequence/acknowledgement numbers and the peer's advertised window,
* relays payload in both directions,
* retransmits unacknowledged segments (with their payload intact),
* sends `FIN`/`RST` correctly on teardown.

### `Proxy.kt`
One `ProxyClient` interface with four implementations. All of them **never resolve hostnames
locally** — literal IPs are sent as IPs and names are sent as names so the upstream server performs
remote DNS resolution.

---

## Limitations (please read)

* **UDP is limited.** DNS is handled through the fake-IP mechanism; other UDP (including QUIC /
  HTTP3) is dropped, and browsers fall back to TCP/HTTP2. YouTube and Google work correctly over
  TCP, but media-heavy sites may take a moment longer to start.
* **You must supply your own server.** See [First run](#first-run--you-need-a-server).
* **Not audited.** The code is readable and tested, but it has not had a professional security
  review. Do not use it as your only protection in a hostile environment.
* **Not on F-Droid / Play Store.** Install manually from the APK.
* **REALITY** is accepted in configuration and the handshake is sent, but the uTLS fingerprint
  emulation is not implemented — use `security=tls` unless you specifically need otherwise.

---

## Contributing

Pull requests are welcome. Please keep the `core` module free of Android dependencies so the
engine stays testable on a JVM, and add a test for any behaviour you change.

```bash
./gradlew :core:test      # must pass
./gradlew :app:assembleRelease
```

---

## License

MIT — see [LICENSE](LICENSE).

## Disclaimer

Falak VPN is a general-purpose network tool. You are responsible for complying with the laws and
regulations that apply to you and to the servers you connect to. It is provided with no warranty of
any kind.
