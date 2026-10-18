# ezyvpn (Android)

WireGuard tabanlı basit bir VPN istemcisi. Kotlin + Jetpack Compose.

## Çalıştırma
1. Android Studio (Koala veya üstü) ile bu klasörü aç, Gradle sync'in bitmesini bekle.
2. Telefonu USB ile bağla (geliştirici modu açık) ya da emülatör kullan, Run'a bas.
3. Uygulamada "Sunucu ekle" ile bir isim ve WireGuard config'ini yapıştır:

```
[Interface]
PrivateKey = <telefonun özel anahtarı>
Address = 10.8.0.2/32
DNS = 1.1.1.1

[Peer]
PublicKey = <sunucunun genel anahtarı>
Endpoint = SUNUCU_IP:51820
AllowedIPs = 0.0.0.0/0, ::/0
PersistentKeepalive = 25
```
4. "Bağlan"a bas, Android'in VPN iznini onayla.

## Sunucu tarafı
Bir VPS'e WireGuard kurman gerekir (ör. wireguard-install betiği). Her telefon için ayrı bir peer/config üretilir.

## APK
Build > Build Bundle(s) / APK(s) > Build APK(s)

## Android Studio olmadan (GitHub Actions)
1. github.com'da ücretsiz hesap aç, yeni bir repo oluştur.
2. Bu klasörün içindekileri (gizli .github klasörü dahil) repoya yükle.
3. Repoda Actions sekmesi > "APK derle" iş akışının bitmesini bekle (~5 dk).
4. Biten çalıştırmanın altındaki "ezyvpn-apk" dosyasını indir, zip'ten çıkan app-debug.apk'yı telefona at ve kur.
