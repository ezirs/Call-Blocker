# Call Blocker - Smart Call Filter

Call Blocker adalah aplikasi Android untuk memblokir panggilan spam secara otomatis berdasarkan aturan khusus (Awalan, Akhiran, Mengandung, Sama Persis). Dilengkapi dengan fitur Whitelist, riwayat log panggilan, serta Backup & Restore aturan secara offline. Aplikasi ini berjalan sepenuhnya secara offline dan tidak memerlukan login.

<img width="250" alt="image" src="https://github.com/user-attachments/assets/c3b00d49-0072-4cd2-a6cc-c4107178b657" />

## Fitur Utama

1. **Filter Cerdas (Smart Blocking Rules)**
   Anda dapat membuat aturan untuk memblokir panggilan. Terdapat 4 jenis aturan yang bisa digunakan:
   - **Awalan:** Memblokir nomor yang dimulai dengan angka tertentu.
   - **Akhiran:** Memblokir nomor yang diakhiri dengan angka tertentu.
   - **Mengandung:** Memblokir nomor yang memiliki deretan angka tertentu di dalamnya.
   - **Sama Persis:** Memblokir nomor spesifik secara penuh.
   
   <img width="250" alt="image" src="https://github.com/user-attachments/assets/66d31152-b9bb-466b-9664-88fc57fcaff6" />

2. **Log Panggilan (Diblokir & Lolos)**
   - **Tab Diblokir:** Melihat daftar riwayat panggilan masuk yang berhasil dicegah / diblokir oleh aplikasi. Anda dapat mengelompokkan log ini untuk melihat statistik panggilan spam yang berulang.
   - **Tab Lolos:** Melihat riwayat panggilan masuk yang tidak terkena aturan blokir dan berhasil masuk ke HP Anda.

   <img width="250" alt="image" src="https://github.com/user-attachments/assets/925601d3-d413-4913-88ae-e2c311bd890d" />

   <img width="250" alt="image" src="https://github.com/user-attachments/assets/51a9aa10-1dfc-43d7-9a34-bb79f7be3f33" />

3. **Whitelist (Daftar Putih)**
   Nomor yang berada di daftar ini **tidak akan pernah diblokir**, meskipun nomor tersebut memiliki pola yang sama dengan kriteria yang ada pada tab Filter. Anda bisa memasukkan nomor-nomor penting secara manual atau memilih langsung (memborong sekaligus) dari daftar Kontak HP Anda.
   
   <img width="250" alt="image" src="https://github.com/user-attachments/assets/1ef85550-bbf8-48bf-b91b-77e444fd4694" />

4. **Pengaturan & Backup Database**
   - **Personalisasi:** Ganti tema aplikasi (Terang/Gelap/Sistem) dan Bahasa antarmuka (Indonesia/Inggris).
   - **Backup & Restore (JSON):** Anda dapat mengekspor (export) seluruh aturan (rules) dan whitelist ke dalam bentuk file berformat `.json`. File ini bisa Anda simpan (backup) atau impor (restore) kapan saja, sangat berguna ketika Anda baru menginstal ulang aplikasi atau pindah ke HP baru.
   
   <img width="250" alt="image" src="https://github.com/user-attachments/assets/d29bbba7-6d14-4f7e-9d05-094b4ca855d0" />

---

## ☕ Dukung Kami (Support)

Jika aplikasi ini membantu Anda terhindar dari spam, dukung pengembangannya melalui:
- **Trakteer**: [trakteer.id/ezirs/tip](https://trakteer.id/ezirs/tip)

---

## Cara Penggunaan

> **Persyaratan Sistem:** Aplikasi ini memerlukan Android versi 10 (Q) atau yang lebih baru agar fitur penyaringan panggilan (Call Screening) dapat berjalan.

1. Buka aplikasi dan tekan tombol bertuliskan **AKTIF / NONAKTIF** di bagian atas (atau ikuti dialog perizinan yang muncul) untuk menjadikan aplikasi ini sebagai *Caller ID & Spam App Default* di pengaturan HP Anda. Ini wajib dilakukan agar aplikasi memiliki akses untuk menyaring panggilan yang masuk.
2. Pergi ke tab **Filter** di navigasi bawah.
3. Masukkan nomor atau pola angka, pilih tipe aturan (misal: Awalan), lalu tekan tombol tambah (`+`).
4. Panggilan masuk berikutnya akan dievaluasi oleh aplikasi secara otomatis. Anda bisa memantau nomor mana saja yang ditolak atau dibiarkan masuk pada tab **Diblokir** dan **Lolos**.

---

## PENTING: Aturan Penulisan Nomor (Khususnya Awalan & Sama Persis)

Agar sistem filter dapat mendeteksi nomor dengan akurat (terutama untuk aturan **Awalan** dan **Sama Persis**), **format nomor yang Anda tulis dalam aturan harus persis seperti bagaimana nomor tersebut terekam dalam Log Panggilan (Riwayat Telepon) di HP Anda.**

Format nomor yang masuk seringkali berbeda-beda tergantung operator atau kode negara (seperti menggunakan `+62`, `08`, dan lain-lain). Berikut adalah panduan agar filter Anda bekerja sempurna:

- **Harus Sesuai Format Asli di Log HP:** 
  Sistem akan membaca nomor apa adanya. Jika panggilan spam masuk dan di log HP Anda tertulis `+6281234567890`, maka sistem Call Blocker membacanya sebagai teks `+6281234567890`.
- **Contoh Penulisan yang SALAH:** 
  Jika nomor yang mengganggu adalah `+6281234567890`, jangan membuat aturan Awalan dengan mengetikkan `081234` atau `81234`. Jika ini dilakukan, aplikasi tidak akan mendeteksinya karena `+628123...` tidak sama dengan `081234...`.
- **Contoh Penulisan yang BENAR:** 
  Untuk nomor contoh di atas, buatlah aturan Awalan dengan mengetik persis dari depan: `+628123`. Jika menggunakan aturan Sama Persis, ketiklah seluruhnya: `+6281234567890`.
- **Hapus Tanda Strip atau Spasi:** 
  Jika Anda menyalin (copy) nomor dan menempelkannya, dan terdapat tanda strip seperti `0812-3456-7890`, pastikan Anda **menghapus tanda strip tersebut**. Anda hanya perlu memasukkan angka saja tanpa pemisah (contoh: `081234567890`). Karakter `+` diperbolehkan jika nomornya memang diawali dengan plus.
- **Untuk Nomor Singkat / Khusus:** 
  Jika Anda ditelepon oleh nomor operator atau layanan singkat (misalnya `188`), maka ketik saja tepat seperti aslinya yaitu `188`.

Dengan mengikuti format log dari perangkat Anda masing-masing, sistem ini akan 100% akurat mencekal nomor yang mengganggu Anda.
