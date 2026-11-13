# R&MA — Customer-Owned Oracle WireGuard Gateway Guide

**Status:** Manual guide only. R&MA does not create cloud resources, change firewall rules, or handle cloud credentials.

This guide is for a private WireGuard gateway on a Linux VM in the user's own Oracle Cloud account. Follow the provider's current console and terms. Free-tier availability and eligibility are not guaranteed.

## Before you start

- Use only an Always Free-eligible VM shape and image shown as eligible in your own OCI console.
- E2.1.Micro is AMD/x86_64 with limited resources. A1 Flex is ARM64; select an ARM64 Ubuntu image and ARM64-compatible packages.
- Verify that the VM has an internet-routable public IPv4 address and determine whether it is ephemeral or reserved. Do not assume an address survives VM deletion/replacement.
- Check the current Always Free quota, region capacity, network egress allowance, and possible charges before creating anything.
- Use a strong SSH key. Do not expose SSH to the whole internet if a narrower source-IP rule is practical.
- Do not paste cloud passwords, API tokens, SSH private keys, or WireGuard private keys into R&MA, chats, or support requests.

## 1. Create the VM and network rules

In OCI, create a Linux VM using an Always Free-eligible shape in the tenancy home region. Assign a public IPv4 only if you understand the allocation type. In the subnet security list or network security group, allow inbound UDP **51820** from the client networks you intend to use (or temporarily from a wider range only if necessary, then narrow it). Keep SSH TCP 22 restricted to your trusted source address. The VM must also be allowed to send outbound traffic.

Do not open every port. The WireGuard port can be changed, but the client and server must match.

## 2. Install WireGuard on Ubuntu

Connect over SSH and run:

```bash
sudo apt update
sudo apt install wireguard iptables -y
```

Check the VM's outbound interface name:

```bash
ip route show default
```

Use the interface shown after `dev` (often `ens3`) in the NAT rules below. Do not assume the name.

## 3. Generate keys on the VM

Run the following on the VM. The private key must remain on the VM and be readable only by root:

```bash
sudo -i
umask 077
wg genkey | tee /etc/wireguard/server_private.key | wg pubkey > /etc/wireguard/server_public.key
cat /etc/wireguard/server_public.key
```

Copy **only the server public key** for the client configuration. Never share the server private key. Generate the client key pair on the client device when possible; only the client public key belongs in the server peer block.

## 4. Configure IPv4 forwarding and NAT

Enable IPv4 forwarding:

```bash
cat >/etc/sysctl.d/99-wireguard-forward.conf <<'EOF'
net.ipv4.ip_forward=1
EOF
sysctl --system
```

Create `/etc/wireguard/wg0.conf` as root, substituting the actual interface name, server private key, and client public key:

```ini
[Interface]
Address = 10.66.66.1/24
ListenPort = 51820
PrivateKey = REPLACE_WITH_SERVER_PRIVATE_KEY
PostUp = iptables -A FORWARD -i wg0 -j ACCEPT; iptables -A FORWARD -o wg0 -j ACCEPT; iptables -t nat -A POSTROUTING -o REPLACE_WITH_EGRESS_INTERFACE -j MASQUERADE
PostDown = iptables -D FORWARD -i wg0 -j ACCEPT; iptables -D FORWARD -o wg0 -j ACCEPT; iptables -t nat -D POSTROUTING -o REPLACE_WITH_EGRESS_INTERFACE -j MASQUERADE

[Peer]
PublicKey = REPLACE_WITH_CLIENT_PUBLIC_KEY
AllowedIPs = 10.66.66.2/32
```

Protect the file and start WireGuard:

```bash
chmod 600 /etc/wireguard/wg0.conf
systemctl enable --now wg-quick@wg0
wg show
```

If the service fails, inspect `systemctl status wg-quick@wg0` and `journalctl -u wg-quick@wg0`. Do not paste private keys into logs or support messages.

## 5. Add the client peer

Create a client configuration on the phone using a client private key generated on that phone:

```ini
[Interface]
PrivateKey = CLIENT_PRIVATE_KEY
Address = 10.66.66.2/32
DNS = 1.1.1.1

[Peer]
PublicKey = SERVER_PUBLIC_KEY
Endpoint = YOUR_VM_PUBLIC_IPV4:51820
AllowedIPs = 0.0.0.0/0
PersistentKeepalive = 25
```

This example routes IPv4 through the gateway. It is **not** a complete IPv6 leak-protection configuration. Until IPv6 routing and firewall behavior have been deliberately configured and tested, do not assume IPv6 is protected; use a client policy that blocks IPv6 outside the tunnel or do not enable this profile for sensitive use.

Import the client configuration into R&MA. Never import the server's private key into the phone.

## 6. Verify the result

1. Confirm `wg show` reports a recent handshake after the phone connects.
2. Confirm RX/TX counters increase when the phone sends traffic.
3. From the phone while the tunnel is active, check the public IPv4 using a reputable IP-check service. It should match the VM's public egress IPv4.
4. Test DNS resolution and IPv4 connectivity.
5. Test IPv6 separately. A successful IPv4 check does not prove there is no IPv6 leak.
6. Disconnect the tunnel and confirm the phone returns to its ordinary network path.
7. Reboot the VM and confirm the service starts and the address remains what the provider console says it is.

A handshake or nonzero RX/TX alone does not prove internet egress works. R&MA must eventually distinguish tunnel-connected from public-egress-verified.

## Operational cautions

- Keep the VM and WireGuard packages updated.
- Restrict SSH and UDP exposure to what is needed; monitor traffic and provider quotas.
- A provider can suspend or reclaim resources under its terms. A public IP may change if the VM or IP allocation is replaced.
- This setup is a personal gateway, not a public proxy service. Do not offer access to unknown third parties.
- Do not use the gateway to evade legal restrictions or a destination service's access rules.
- This guide is a baseline, not a guarantee of provider compatibility. Firewall defaults, interface names, IPv6, and OCI networking differ by image and region.
- No VM or charge has been created by R&MA or by this guide.
