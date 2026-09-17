# Baş Zincir - Satış (Android Uygulaması)

Bu klasör, sizin için hazırlanmış tam bir Android Studio / Gradle projesidir.
Kodun tamamı yazılmıştır; sizin yapmanız gereken tek şey **Firebase'i
bağlamak** ve **GitHub'a yüklemek**tir. Derleme (APK üretimi) GitHub
Actions üzerinde otomatik olarak yapılır — bilgisayarınıza Android Studio
kurmanıza gerek yoktur.

## Uygulama Ne Yapıyor?

- **Teklif Hazırla** ve **Proforma Fatura Hazırla** olmak üzere iki ana bölüm
- Tüm veriler (müşteriler, ürünler, standartlar, teklifler, proformalar)
  **Firebase Firestore**'da saklanır — telefon değiştirseniz bile veriler
  kaybolmaz, birden fazla cihazdan erişilebilir
- Müşteri listeniz (357 firma) uygulamaya gömülüdür; ilk açılışta otomatik
  olarak Firestore'a yüklenir
- Teklif ve proforma faturalar, gönderdiğiniz örnek Word dosyalarındaki
  antet, imza ve kırmızı banka bilgileri ile **PDF** olarak üretilir
- PDF, dosya adı "**Müşteri Adı - Teklif.pdf**" / "**Müşteri Adı - Proforma
  Fatura.pdf**" şeklinde hem uygulama içine hem de mümkünse telefonun
  **İndirilenler** klasörüne kaydedilir, ardından WhatsApp/E-posta gibi bir
  uygulamayla doğrudan müşteriye gönderebileceğiniz paylaşım ekranı açılır
- Müşteri adı, şehir, vergi dairesi gibi alanlar otomatik **BÜYÜK HARF**
  yazılır
- Herhangi bir müşteriye tıklayınca geçmiş teklif/proforma kayıtları listelenir,
  düzenlenebilir veya silinebilir
- **Giriş zorunludur.** Sadece Firebase Console'da sizin oluşturduğunuz 4
  e-posta/şifre hesabı uygulamayı kullanabilir; başka kimse veriye erişemez

---

## 1. Adım: Firebase Projesi Oluşturma (5 dakika)

1. https://console.firebase.google.com adresine gidin, Google hesabınızla
   giriş yapın.
2. **"Proje ekle" (Add project)** deyin, ismini `Bas Zincir Satis` gibi bir
   şey yapın, ilerleyin ve projeyi oluşturun.
3. Sol menüden **Build > Firestore Database** açın, **"Veritabanı oluştur"**
   deyin, bir bölge seçin (örn. `eur3 (europe-west)`), ve
   **"üretim modu" (production mode)** ile başlatın.
4. Firestore açıldıktan sonra üstteki **"Kurallar" (Rules)** sekmesine
   girin, bu projedeki `firestore.rules` dosyasının içeriğini kopyalayıp
   yapıştırın ve **"Yayınla" (Publish)** deyin. Bu kurallar, veritabanına
   sadece aşağıda 6. adımda oluşturacağınız 4 e-posta hesabının erişebilmesini
   sağlar — başka hiç kimse (APK'yı ele geçirse bile) okuma veya yazma
   yapamaz.
5. Sol menüden **Build > Authentication** açın, **"Get started"** deyin,
   "Sign-in method" sekmesinden **Email/Password (E-posta/Şifre)**
   sağlayıcısını etkinleştirin. (Anonim girişi AÇMAYIN — uygulama artık
   sadece belirli, önceden oluşturulmuş hesaplarla çalışır.)
6. Aynı Authentication ekranında **"Users" (Kullanıcılar)** sekmesine geçin,
   **"Add user" (Kullanıcı ekle)** butonuna basarak aşağıdaki 4 hesabı,
   her biri için kendi belirleyeceğiniz bir şifreyle tek tek oluşturun:
   - pdrercumentessiz@gmail.com
   - baszincirosb@gmail.com
   - baszincir@gmail.com
   - pazarlama2@baszincir.com.tr

   Şifreleri güvenli bir şekilde ilgili kişilerle paylaşın; uygulamada
   "şifremi unuttum" veya "kayıt ol" seçeneği bilerek yoktur — sadece burada
   oluşturduğunuz 4 hesap giriş yapabilir. İleride bir hesabı kaldırmak
   isterseniz bu ekrandan silmeniz yeterlidir.
7. Sol üstteki dişli simgesinden **"Proje ayarları"**na girin, "Uygulamalarınız"
   bölümünde **Android simgesine** tıklayıp yeni bir Android uygulaması
   ekleyin:
   - **Android paket adı:** `com.baszincir.satis` (tam olarak bu şekilde yazın)
   - Takma ad: `Baş Zincir - Satış`
   - "Kaydet" deyip **`google-services.json`** dosyasını indirin. Bu dosyayı
     bilgisayarınızda bir yere kaydedin, birazdan lazım olacak.

---

## 2. Adım: GitHub Deposu Oluşturma

1. https://github.com adresinde yeni ve **boş** bir repo oluşturun
   (örn. `bas-zincir-satis`), README/gitignore eklemeden.
2. Bu klasördeki (`bas-zincir-satis/`) tüm dosyaları o depoya yükleyin.
   Bilgisayarınızda Git yüklüyse:
   ```bash
   cd bas-zincir-satis
   git init
   git add .
   git commit -m "İlk sürüm"
   git branch -M main
   git remote add origin https://github.com/KULLANICI_ADINIZ/bas-zincir-satis.git
   git push -u origin main
   ```
   Git kullanmıyorsanız, GitHub'ın web arayüzündeki **"Add file > Upload
   files"** ile klasördeki tüm dosya ve alt klasörleri (gizli `.github`
   klasörü dahil) sürükleyip bırakabilirsiniz.

   > **Not:** `google-services.json` dosyasını **repoya yüklemeyin** —
   > içinde Firebase proje bilgileriniz var, bir sonraki adımda bunu güvenli
   > bir şekilde (GitHub Secret olarak) ekleyeceğiz. `.gitignore` dosyası
   > zaten bu dosyayı hariç tutacak şekilde ayarlanmıştır.

---

## 3. Adım: google-services.json'ı GitHub'a Güvenle Ekleme

GitHub Actions'ın derleme sırasında Firebase'e bağlanabilmesi için
`google-services.json` dosyasının içeriğini bir "Secret" (gizli değişken)
olarak eklemeniz gerekiyor:

1. İndirdiğiniz `google-services.json` dosyasını **base64** formatına
   çevirin:
   - Mac/Linux: Terminal'de `base64 -i google-services.json | pbcopy`
     (Mac) veya `base64 -w0 google-services.json` (Linux) yazın, çıkan
     metni kopyalayın.
   - Windows (PowerShell):
     ```powershell
     [Convert]::ToBase64String([IO.File]::ReadAllBytes("google-services.json")) | Set-Clipboard
     ```
   - Ya da https://www.base64encode.org gibi bir siteye dosyayı yükleyip
     çıkan metni kopyalayabilirsiniz.
2. GitHub'daki reponuzda **Settings > Secrets and variables > Actions**
   sekmesine gidin, **"New repository secret"** deyin.
3. İsim (Name) alanına tam olarak: `GOOGLE_SERVICES_JSON_BASE64`
4. Değer (Secret) alanına, kopyaladığınız base64 metnini yapıştırın, kaydedin.

---

## 4. Adım: APK'yı Derletme

1. GitHub reponuzda üstteki **"Actions"** sekmesine gidin.
2. "APK Derle" iş akışını göreceksiniz (kodu push ettiğinizde otomatik
   başlar; başlamadıysa **"Run workflow"** butonuyla elle de
   tetikleyebilirsiniz).
3. İşlem bitince (birkaç dakika sürer, yeşil tik ✅ görünür), o çalışmanın
   sayfasını açın, en altta **"Artifacts"** bölümünden
   **`bas-zincir-satis-debug-apk`** dosyasını indirin. İçinden
   `app-debug.apk` çıkacak.
4. Bu APK dosyasını telefonunuza (WhatsApp Web, e-posta, Google Drive, USB
   kablo — hangisi kolayınıza geliyorsa) aktarın, telefonda dosyaya
   dokunup kurun. Android "bilinmeyen kaynaklardan yükleme" izni
   isteyebilir; buna izin verin.
5. Uygulama açıldığında önce bir **giriş ekranı** karşınıza çıkacak. 1. Adımda
   Firebase Console'da oluşturduğunuz 4 hesaptan biriyle (e-posta + şifre)
   giriş yapın. Giriş yaptıktan sonra uygulama birkaç saniye içinde 357
   müşteriyi otomatik olarak Firebase'e yükleyecektir (sadece ilk girişte,
   bir kereliğine).

Kodda değişiklik yaptıkça (veya ben sizin için güncelleme gönderdikçe),
GitHub'a her `push` işleminde APK otomatik olarak yeniden derlenir; yukarıdaki
4. adımı tekrarlayıp güncel APK'yı indirip telefonunuza kurmanız yeterli.

---

## Uygulamayı Kullanma

- **Giriş**: Uygulama açıldığında Firebase Console'da oluşturduğunuz 4
  hesaptan biriyle giriş yapmanız gerekir. Başka hiç kimse (APK dosyasını
  ele geçirse bile) giriş yapamaz. Ayarlar ekranından **"Çıkış Yap"**
  diyerek hesabınızdan çıkabilirsiniz.
- **Teklif Hazırla**: Müşteri seçin (veya "+" ile yeni ekleyin), şehir,
  tarih, zincir türü, adet, standart, miktar/birim, fiyat, KDV, nakliye,
  ödeme şekli, teslimat süresi ve opsiyon tarihini girin. "Kaydet ve PDF
  Oluştur" dediğinizde PDF üretilir ve paylaşım ekranı açılır.
- **Proforma Fatura Hazırla**: Müşteri seçtiğinizde şehir/vergi dairesi/
  vergi no otomatik dolar. Proforma No ve Sipariş No otomatik üretilir.
  Birden fazla ürün kalemi ekleyebilirsiniz; toplam, KDV %20 ve genel
  toplam otomatik hesaplanır.
- **Müşteriler** (sağ üstteki simge): Müşteri arayın, birine dokunduğunuzda
  o müşterinin geçmiş tekliflerini ve proformalarını görürsünüz; PDF
  simgesine dokunarak tekrar PDF üretip gönderebilirsiniz. Kayda dokunarak
  düzenleyebilir, formdaki çöp kutusu simgesiyle silebilirsiniz.
- Zincir türü ve Standart listeleri boş başlar; formda "+ yeni ekle" ile
  girdiğiniz her yeni ürün/standart bir daha kullanmak üzere listeye
  eklenir.

---

## Bilinen Sınırlar / Sonraki Adımlar

Bu, üzerinde çalışılabilir sağlam bir ilk sürümdür. Aşağıdaki noktalarda
gerçek cihazda test ettikten sonra ince ayar gerekebilir — bana bildirirseniz
düzeltirim:

- PDF sayfa düzeni (yazı tipi boyutları, satır aralıkları) örnek Word
  dosyalarına yakın olacak şekilde koda döküldü; gerçek bir teklif/proforma
  ile karşılaştırıp küçük konum ayarları isteyebilirsiniz.
- Uygulama tek kullanıcı (siz) için tasarlandı; birden fazla kişi aynı anda
  kullanacaksa haber verin, kullanıcı girişi (email/şifre) ekleyebilirim.
- İlk derleme GitHub Actions üzerinde gerçekleşeceği için olası küçük
  derleme hatalarını (varsa) Actions sekmesindeki kırmızı ❌ işaretine
  tıklayıp bana log'unu ileterek hızlıca düzeltebiliriz.

Herhangi bir adımda takılırsanız veya bir ekranı/metni değiştirmemi
isterseniz yazmanız yeterli.
