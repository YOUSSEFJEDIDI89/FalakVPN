# Architecture notes

## Why a userspace TCP stack?

Android's `VpnService` hands you **raw IP packets** from a `tun` interface. You are responsible for
everything above IP. There are three ways to deal with that:

1. **Ship a native library** (`libtun2socks.so`, `libv2ray.so`). Fast, but it is an opaque binary
   blob you cannot audit, and it is the single largest source of supply-chain risk in most Android
   VPN clients.
2. **Bundle a huge framework** (clash-core, sing-box). Powerful but hundreds of megabytes.
3. **Write the stack yourself.** That is what Falak VPN does. The `core` module is ~2,500 lines of
   Kotlin with no Android dependencies, which means it runs and is testable on a plain JVM.

The trade-off is real: a from-scratch stack has fewer protocol edge cases than a battle-tested
production stack. We mitigate that with an integration test that drives a **real TLS session to
YouTube** through the tunnel.

## Module boundaries

```
core/    pure Kotlin/JVM    no android.* imports at all
app/     Android            VpnService + Compose UI
```

The `core` module depends only on the JDK and the Kotlin stdlib. This is enforced by keeping all
Android-specific concerns (tun file descriptors, `protect()`, notifications, per-app firewall) in
`app/`.

## The `SocketProtector` seam

`ProxyClient` implementations open a socket to the upstream server. Once the tunnel is up, that
socket must be excluded from the VPN routes, otherwise the tunnel tunnels its own transport and
deadlocks. `SocketProtector` is the seam:

```kotlin
fun interface SocketProtector {
    fun protect(socket: Socket): Boolean
}
```

In `app/`, it is implemented with `VpnService.protect()`. In tests, it is a no-op.

## Sequence number handling

TCP sequence numbers are 32-bit and wrap. The implementation keeps them in `Long` and masks to 32
bits when building packets (`seq and 0xFFFFFFFFL`).

A SYN consumes one sequence number, so the stack advances its counter when it sends the SYN|ACK.
Getting this wrong is subtle: the first data segment would overlap the handshake and the peer would
silently drop it.

## Window handling

The stack advertises a fixed 65,535-byte window to the device and tracks the device's advertised
window separately. Data is only sent while `window - inFlight > 0`. The initial `deviceAckedSeq` is
set to the current send sequence so the first segment is not blocked by an apparent zero window
before the device's ACK arrives.

## ACK storms

A pure ACK from the peer (no payload, no SYN, no FIN) is **never** answered with another ACK. Both
sides would otherwise bounce empty ACKs forever, burning CPU and battery. This was caught by the
integration test as a `StackOverflowError` from unbounded recursion.

## Fake-IP DNS

The app never lets the device resolve names locally. DNS queries to the virtual resolver are
answered with addresses from `198.18.0.0/15`, with a reverse map kept in memory. When the device
opens a TCP connection to such an address, the stack recovers the hostname and hands the *name* (not
an address) to the upstream proxy, so resolution happens on the server.

## Retransmission

Unacknowledged segments are kept with their payload. On a timer, stale segments are re-sent **with
their data**. Storing only headers - an early bug - silently corrupted streams on packet loss.
