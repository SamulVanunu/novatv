# P2 açılış ölçümü (TV emülatörü)

Ölçüm: `tv36` AVD (API 36, 1920×1080 @ 320 dpi), mock Xtream + M3U + Stalker
kaynakları yüklü, 8 kanal ve izleme geçmişi dolu.

Komut:

```bash
adb shell am force-stop dev.anon.novatv
adb shell am start -W -n dev.anon.novatv/.app.MainActivity
```

| Deneme | TotalTime | WaitTime | Not |
| --- | --- | --- | --- |
| 1 | 1845 ms | 1885 ms | süreç yeni öldürüldü, soğuk |
| 2 | 1050 ms | 1066 ms | |
| 3 | 1072 ms | 1099 ms | |

Hedef `< 2000 ms` karşılandı. Son izlenen kanal (`TRT 1`) açılışta doğrudan
oynatıcıya gidiyor; bu süreye kaynak/kimlik çözümü ve ilk kare dahildir.

Not: emülatörde ilk açılış JIT/derleme nedeniyle daha yavaştır; fiziksel
cihazda soğuk açılışın daha düşük olması beklenir. Ölçüm tek oturumluktur,
regresyon takibi için aynı komutla tekrarlanmalıdır.
