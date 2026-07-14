with open("app/README.md", "r") as f:
    text = f.read()

old_desc = """ShieldCall (Call Blocker) adalah aplikasi pemblokir panggilan spam pintar yang memungkinkan Anda menyaring panggilan masuk berdasarkan aturan-aturan spesifik yang Anda tentukan sendiri. Aplikasi ini sangat berguna untuk memblokir nomor penipuan, telemarketing, atau nomor yang tidak dikenal secara otomatis sebelum HP Anda berdering."""
new_desc = """ShieldCall (Call Blocker) adalah aplikasi Android untuk memblokir panggilan spam secara otomatis berdasarkan aturan khusus (Awalan, Akhiran, Mengandung, Sama Persis). Dilengkapi dengan fitur Whitelist, riwayat log panggilan, serta Backup & Restore aturan secara offline. Aplikasi ini berjalan sepenuhnya secara offline dan tidak memerlukan login."""

if old_desc in text:
    text = text.replace(old_desc, new_desc)
    with open("app/README.md", "w") as f:
        f.write(text)
    print("Replaced successfully")
else:
    print("Could not find the text to replace")
