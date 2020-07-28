firewall {
    all-ping enable
    broadcast-ping disable
    group {
        address-group LANS {
            address 192.168.1.0/24
            address 192.168.2.0/24
            address 192.168.3.0/24
            address 192.168.4.0/24
            description "All internal LANs"
        }
    }
    ipv6-receive-redirects disable
    ipv6-src-route disable
    ip-src-route disable
    log-martians enable
    name IOT-Local {
        default-action drop
        description "Block access to router except for DNS and DHCP"
        rule 1 {
            action accept
            description "Allow DNS to Router"
            destination {
                group {
                    address-group ADDRv4_eth0
                }
                port 53
            }
            log disable
            protocol udp
        }
        rule 2 {
            action accept
            description "Allow DHCP to Router"
            destination {
                group {
                    address-group ADDRv4_eth0
                }
                port 67,68
            }
            log disable
            protocol udp
        }
        rule 3 {
            action drop
            description "Drop invalid"
            log disable
            protocol all
            state {
                established disable
                invalid enable
                new disable
                related disable
            }
        }
    }
    name PROTECT_LANS {
        default-action accept
        description "default allow to internet"
        rule 1 {
            action drop
            description "drop traffic between LANs"
            destination {
                group {
                    address-group LANS
                }
            }
            log disable
            protocol all
        }
        rule 2 {
            action drop
            description "Drop invalid"
            log disable
            protocol all
            state {
                established disable
                invalid enable
                new disable
                related disable
            }
        }
    }
    name Unacceptable_OUT {
        default-action accept
        description "ports blocked from leaving IOT network"
        rule 1 {
            action drop
            description "Drop Dangerous Outbound TCP Traffic"
            destination {
                port 135,137-139,445,6660-6669
            }
            log disable
            protocol tcp
        }
        rule 3 {
            action drop
            description "Drop All Dangerous Outbound UDP Traffic"
            destination {
                port 69,135,137-139,161,162,514
            }
            log disable
            protocol udp
        }
        rule 4 {
            action drop
            description "Drop All Invalid"
            log disable
            protocol all
            state {
                established disable
                invalid enable
                new disable
                related disable
            }
        }
    }
    name WAN_IN {
        default-action drop
        description "WAN to internal"
        rule 10 {
            action accept
            description "Allow established/related"
            state {
                established enable
                related enable
            }
        }
        rule 20 {
            action drop
            description "Drop invalid state"
            state {
                invalid enable
            }
        }
    }
    name WAN_LOCAL {
        default-action drop
        description "WAN to router"
        rule 10 {
            action accept
            description "Allow established/related"
            state {
                established enable
                related enable
            }
        }
        rule 20 {
            action drop
            description "Drop invalid state"
            state {
                invalid enable
            }
        }
    }
    receive-redirects disable
    send-redirects enable
    source-validation disable
    syn-cookies enable
}
interfaces {
    ethernet eth0 {
        address dhcp
        description Internet
        duplex auto
        firewall {
            in {
                name WAN_IN
            }
            local {
                name WAN_LOCAL
            }
        }
        mac 18:e8:29:2d:15:24
        speed auto
    }
    ethernet eth1 {
        address 192.168.1.1/24
        description "Home Net"
        duplex auto
        firewall {
            in {
                name PROTECT_LANS
            }
            out {
                name Unacceptable_OUT
            }
        }
        speed auto
    }
    ethernet eth2 {
        address 192.168.2.1/24
        description "IoT Video"
        duplex auto
        firewall {
            in {
                name PROTECT_LANS
            }
            local {
                name IOT-Local
            }
            out {
                name Unacceptable_OUT
            }
        }
        speed auto
    }
    ethernet eth3 {
        address 192.168.3.1/24
        description "IoT Non-video"
        duplex auto
        firewall {
            in {
                name PROTECT_LANS
            }
            local {
                name IOT-Local
            }
            out {
                name Unacceptable_OUT
            }
        }
        speed auto
    }
    ethernet eth4 {
        address 192.168.4.1/24
        description VPN
        duplex auto
        firewall {
            in {
                name PROTECT_LANS
            }
            out {
                name Unacceptable_OUT
            }
        }
        poe {
            output off
        }
        speed auto
    }
    loopback lo {
    }
    switch switch0 {
        description Local
        mtu 1500
        switch-port {
            vlan-aware disable
        }
    }
}
port-forward {
    auto-firewall enable
    hairpin-nat enable
    lan-interface eth1
    lan-interface eth2
    lan-interface eth3
    lan-interface eth4
    wan-interface eth0
}
service {
    dhcp-server {
        disabled false
        hostfile-update disable
        shared-network-name iot-video {
            authoritative disable
            subnet 192.168.2.0/24 {
                default-router 192.168.2.1
                dns-server 208.67.220.123
                dns-server 208.67.222.123
                lease 86400
                start 192.168.2.20 {
                    stop 192.168.2.245
                }
            }
        }
        shared-network-name iot-non-video {
            authoritative disable
            subnet 192.168.3.0/24 {
                default-router 192.168.3.1
                dns-server 208.67.220.123
                dns-server 208.67.222.123
                lease 86400
                start 192.168.3.20 {
                    stop 192.168.3.245
                }
            }
        }
        shared-network-name private-vpn {
            authoritative disable
            subnet 192.168.4.0/24 {
                default-router 192.168.4.1
                dns-server 8.8.8.8
                dns-server 4.4.4.4
                lease 86400
                start 192.168.4.20 {
                    stop 192.168.4.245
                }
            }
        }
        shared-network-name protected {
            authoritative enable
            subnet 192.168.1.0/24 {
                default-router 192.168.1.1
                dns-server 192.168.1.1
                lease 86400
                start 192.168.1.20 {
                    stop 192.168.1.245
                }
            }
        }
        static-arp disable
        use-dnsmasq disable
    }
    dns {
        forwarding {
            cache-size 150
            listen-on switch0
            listen-on eth1
            listen-on eth2
            listen-on eth3
            listen-on eth4
        }
    }
    gui {
        http-port 80
        https-port 443
        older-ciphers enable
    }
    nat {
        rule 5010 {
            description "masquerade for WAN"
            outbound-interface eth0
            type masquerade
        }
    }
    ssh {
        port 22
        protocol-version v2
    }
    ubnt-discover {
        disable
    }
    unms {
        disable
    }
}
system {
    host-name erx
    ipv6 {
        disable
    }
    login {
        user ubnt {
            authentication {
                plaintext-password "ubnt"
            }
            full-name ""
            level admin
        }
    }
    ntp {
        server 0.ubnt.pool.ntp.org {
        }
        server 1.ubnt.pool.ntp.org {
        }
        server 2.ubnt.pool.ntp.org {
        }
        server 3.ubnt.pool.ntp.org {
        }
    }
    syslog {
        global {
            facility all {
                level notice
            }
            facility protocols {
                level debug
            }
        }
    }
    time-zone America/Chicago
}


/* Warning: Do not remove the following line. */
/* === vyatta-config-version: "config-management@1:conntrack@1:cron@1:dhcp-relay@1:dhcp-server@4:firewall@5:ipsec@5:nat@3:qos@1:quagga@2:suspend@1:system@4:ubnt-pptp@1:ubnt-udapi-server@1:ubnt-unms@1:ubnt-util@1:vrrp@1:vyatta-netflow@1:webgui@1:webproxy@1:zone-policy@1" === */
/* Release version: v2.0.8-hotfix.1.5278088.200305.1641 */
